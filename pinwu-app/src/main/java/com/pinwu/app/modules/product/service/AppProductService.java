package com.pinwu.app.modules.product.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.pinwu.app.modules.product.domain.doc.ProductDoc;
import com.pinwu.app.modules.product.domain.dto.ProductPublishDto;
import com.pinwu.app.modules.product.domain.dto.ProductSearchQuery;
import com.pinwu.app.modules.product.domain.dto.ProductSkuDto;
import com.pinwu.app.modules.product.domain.entity.PwProduct; // 你的MySQL实体
import com.pinwu.app.modules.product.domain.entity.PwProductSku;
import com.pinwu.app.modules.product.domain.vo.ProductDetailVo;
import com.pinwu.app.modules.product.mapper.PwProductMapper;
import com.pinwu.app.modules.product.mapper.PwProductSkuMapper;
import com.pinwu.app.modules.user.domain.PwUser;
import com.pinwu.app.modules.user.mapper.PwUserMapper;
import com.pinwu.common.core.redis.RedisCache;
import com.pinwu.common.exception.ServiceException;
import org.elasticsearch.common.lucene.search.function.CombineFunction;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.common.lucene.search.function.FunctionScoreQuery;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AppProductService {

    @Autowired
    private PwProductMapper productMapper;

    @Autowired
    private PwProductSkuMapper skuMapper;

    @Autowired
    private ElasticsearchRestTemplate esTemplate;


    @Autowired
    private RedisCache redisCache;
    @Autowired
    private PwUserMapper userMapper; // 需要查卖家

    /**
     * 1. 发布商品 (MySQL + ES 双写)
     */
    @Transactional(rollbackFor = Exception.class)
    public void publish(ProductPublishDto dto, Long userId) {
        // 1.1 存入 MySQL
        PwProduct product = new PwProduct();
        BeanUtil.copyProperties(dto, product);
        if (dto.getTags() != null) {
            product.setTags(JSONUtil.toJsonStr(dto.getTags()));
        }
        product.setSellerId(userId);
        product.setStatus(1); // 默认上架
        product.setCreateTime(new Date());
        product.setViewCount(0);
        product.setLikeCount(0);
        
        // 这里的 insert 必须返回主键 ID，检查你的 XML 配置 useGeneratedKeys="true"
        productMapper.insertPwProduct(product);

        // 1.2 自动生成默认 SKU (C2C 默认单规格)
        List<PwProductSku> skuEntityList = new ArrayList<>();

        if (dto.getSkuList() != null && !dto.getSkuList().isEmpty()) {
            // 情况 A: 多规格 (如二手书: 高数, 英语)
            for (ProductSkuDto skuDto : dto.getSkuList()) {
                PwProductSku sku = new PwProductSku();
                BeanUtil.copyProperties(skuDto, sku);
                sku.setProductId(product.getId()); // ★ 关联主表ID
                if (sku.getStock() == null) sku.setStock(1); // 默认库存1
                skuEntityList.add(sku);
            }
        } else {
            // 情况 B: 单规格 (如二手手机) - 自动生成一个默认SKU
            // 这一步非常重要！统一了后续的订单逻辑，所有订单都指向SKU。
            PwProductSku defaultSku = new PwProductSku();
            defaultSku.setProductId(product.getId());
            defaultSku.setSkuName("默认"); // 或者用商品标题
            defaultSku.setPrice(product.getPrice());
            defaultSku.setStock(1); // 默认1件
            defaultSku.setSkuPic(product.getPic());
            skuEntityList.add(defaultSku);
        }

        // 批量插入子表
        skuMapper.insertBatch(skuEntityList);

        // 1.3 存入 ES
        ProductDoc doc = new ProductDoc();
        // 拷贝基础属性
        BeanUtil.copyProperties(dto, doc);
        // 补全特殊属性
        doc.setId(product.getId()); // 拿到 MySQL 生成的 ID
        doc.setStatus(1);
        doc.setViewCount(0);
        doc.setCreateTime(new Date());
        
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            doc.setLocation(new GeoPoint(dto.getLatitude(), dto.getLongitude()));
        }

        esTemplate.save(doc);
    }

    /**
     * 2. 首页搜索 (混合排序核心)
     */
    public List<ProductDoc> search(ProductSearchQuery query) {
        // --- A. 构建基础过滤 (Bool) ---
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.filter(QueryBuilders.termQuery("status", 1)); // 必须是上架状态

        if (StrUtil.isNotBlank(query.getKeyword())) {
            // 关键词匹配 (至少匹配70%的内容)
            boolQuery.must(QueryBuilders.multiMatchQuery(query.getKeyword())
                    .field("title", 2.0f) // ★ 显式设置权重 boost = 2.0
                    .field("detail")      // 默认权重 1.0
                    .field("tags")        // 默认权重 1.0
                    .minimumShouldMatch("70%"));
        }

        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withPageable(PageRequest.of(query.getPageNum() - 1, query.getPageSize()));

        // --- B. 排序策略 ---
        // 1. 价格优先
        if (query.getSort() != null && query.getSort() == 1) {
            queryBuilder.withQuery(boolQuery);
            queryBuilder.withSort(SortBuilders.fieldSort("price").order(SortOrder.ASC));
        }
        // 2. 最新发布
        else if (query.getSort() != null && query.getSort() == 2) {
            queryBuilder.withQuery(boolQuery);
            queryBuilder.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
        }
        // 3. 综合排序 (4D 混合算分) - 最复杂的部分
        else {
            // 只有当经纬度都存在时，才启用地理位置加权，否则降级为普通排序
            if (query.getLatitude() != null && query.getLongitude() != null) {
                // Function Score 构造器
                FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions = {
                        // (1) 地理位置衰减
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.gaussDecayFunction("location",
                                        // ★★★ 修复点：使用全限定名，调用 ES 原生对象 ★★★
                                        new org.elasticsearch.common.geo.GeoPoint(query.getLatitude(), query.getLongitude()),
                                        "50km",
                                        "5km",
                                        0.5)
                        ),
                        // (2) 时间衰减
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.gaussDecayFunction("createTime",
                                        "now",
                                        "7d",
                                        "0d",
                                        0.8)
                        ),
                        // (3) 热度加权
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                ScoreFunctionBuilders.fieldValueFactorFunction("viewCount")
                                        .modifier(FieldValueFactorFunction.Modifier.LOG1P)
                                        .factor(0.1f)
                        )
                };

                // 组装 FunctionScore
                FunctionScoreQueryBuilder functionScoreQuery = QueryBuilders.functionScoreQuery(boolQuery, functions)
                        .scoreMode(FunctionScoreQuery.ScoreMode.MULTIPLY)
                        .boostMode(CombineFunction.MULTIPLY);

                queryBuilder.withQuery(functionScoreQuery);
            } else {
                // 如果没传坐标，就只用基础查询 + 时间倒序
                queryBuilder.withQuery(boolQuery);
                queryBuilder.withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC));
            }
        }

        // --- C. 执行搜索 ---
        NativeSearchQuery searchQuery = queryBuilder.build();
        SearchHits<ProductDoc> searchHits = esTemplate.search(searchQuery, ProductDoc.class);

        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }


    /**
     * 获取商品详情 (高并发 + 隐私脱敏)
     */
    public ProductDetailVo getDetail(Long id) {
        String cacheKey = "product:detail:" + id;

        // 1. 查 Redis 缓存
        ProductDetailVo vo = redisCache.getCacheObject(cacheKey);
        if (vo != null) {
            // ★ 异步增加浏览量 (面试加分项: 不直接刷库，先刷Redis，定时任务同步DB)
            redisCache.redisTemplate.opsForValue().increment("product:view:" + id);
            return vo;
        }

        // 2. 查 MySQL 主表 (SPU)
        PwProduct product = productMapper.selectPwProductById(id);
        if (product == null) {
            return null; // 商品不存在
        }

        // 3. 组装 VO
        vo = new ProductDetailVo();
        BeanUtil.copyProperties(product, vo);

        // 处理 Tags (JSON String -> List)
        if (StrUtil.isNotBlank(product.getTags())) {
            vo.setTags(JSONUtil.toList(product.getTags(), String.class));
        }

        // 4. 查 SKU 子表
        List<PwProductSku> skus = skuMapper.selectSkuByProductId(id);
        vo.setSkuList(skus);

        // 5. 查卖家信息 & 脱敏 (Privacy)
        PwUser seller = userMapper.selectPwUserById(product.getSellerId());
        if (seller != null) {
            ProductDetailVo.SellerVo sellerVo = new ProductDetailVo.SellerVo();
            sellerVo.setUserId(seller.getId());
            sellerVo.setNickname(seller.getNickname());
            sellerVo.setAvatar(seller.getAvatar());
            // 简单处理活跃时间
            sellerVo.setActiveTimeText("近期活跃");

            // ★★★ 核心隐私保护：脱敏联系方式 ★★★
            // 即使缓存泄露，拿到的也是 138****1234
            sellerVo.setContactType(product.getContactType());
            if (StrUtil.isNotBlank(product.getContactValue())) {
                // 如果是手机号 (contactType=0)，进行脱敏
                if (product.getContactType() != null && product.getContactType() == 0) {
                    sellerVo.setContactValue(DesensitizedUtil.mobilePhone(product.getContactValue()));
                } else {
                    // 微信号也稍微遮一下
                    sellerVo.setContactValue(StrUtil.hide(product.getContactValue(), 1, 3));
                }
            }
            vo.setSeller(sellerVo);
        }

        // 6. 回写 Redis (设置1小时过期，防止冷门商品长期占用内存)
        redisCache.setCacheObject(cacheKey, vo, 1, TimeUnit.HOURS);

        return vo;
    }

    /**
     * 获取真实联系方式 (需鉴权 + 风控)
     */
    public String getContact(Long productId, Long userId) {
        // 1. 查数据库 (不查缓存，确保实时和安全)
        PwProduct product = productMapper.selectPwProductById(productId);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }

        // 2. 简单的风控 (Redis计数)
        // Key: risk:contact:uid:1001:20251210
        String date = cn.hutool.core.date.DateUtil.format(new Date(), "yyyyMMdd");
        String riskKey = "risk:contact:uid:" + userId + ":" + date;

        long count = redisCache.redisTemplate.opsForValue().increment(riskKey);
        if (count == 1) {
            redisCache.expire(riskKey, 1, TimeUnit.DAYS);
        }

        // 限制每天只能看 10 个不同商品的电话
        if (count > 10) {
            throw new ServiceException("今日查看次数已达上限，请明天再来");
        }

        // 3. 返回真实数据
        return product.getContactValue();
    }
}