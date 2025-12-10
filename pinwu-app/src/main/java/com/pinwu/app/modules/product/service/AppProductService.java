package com.pinwu.app.modules.product.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.pinwu.app.modules.product.domain.doc.ProductDoc;
import com.pinwu.app.modules.product.domain.dto.ProductPublishDto;
import com.pinwu.app.modules.product.domain.dto.ProductSearchQuery;
import com.pinwu.app.modules.product.domain.entity.PwProduct; // 你的MySQL实体
import com.pinwu.app.modules.product.mapper.PwProductMapper;
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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppProductService {

    @Autowired
    private PwProductMapper productMapper;

    @Autowired
    private ElasticsearchRestTemplate esTemplate;

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

        // 1.2 存入 ES
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
            boolQuery.must(QueryBuilders.matchQuery("title", query.getKeyword()).minimumShouldMatch("70%"));
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
}