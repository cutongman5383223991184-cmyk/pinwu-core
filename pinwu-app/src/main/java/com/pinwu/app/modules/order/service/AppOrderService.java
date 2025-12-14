package com.pinwu.app.modules.order.service;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.PageHelper;
import com.pinwu.app.modules.order.domain.dto.OrderConfirmDto;
import com.pinwu.app.modules.order.domain.dto.OrderCreateDto;
import com.pinwu.app.modules.order.domain.dto.OrderListQuery;
import com.pinwu.app.modules.order.domain.entity.PwOrder;
import com.pinwu.app.modules.order.domain.entity.PwOrderItem;
import com.pinwu.app.modules.order.domain.vo.OrderConfirmVo;
import com.pinwu.app.modules.order.domain.vo.OrderListVo;
import com.pinwu.app.modules.order.mapper.PwOrderItemMapper;
import com.pinwu.app.modules.order.mapper.PwOrderMapper;
import com.pinwu.app.modules.product.domain.entity.PwProduct;
import com.pinwu.app.modules.product.domain.entity.PwProductSku;
import com.pinwu.app.modules.product.mapper.PwProductMapper;
import com.pinwu.app.modules.product.mapper.PwProductSkuMapper;
import com.pinwu.common.exception.ServiceException;
import com.pinwu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppOrderService {

    @Autowired
    private PwProductMapper productMapper;

    @Autowired
    private PwProductSkuMapper skuMapper;

    @Autowired
    private PwOrderMapper orderMapper;      // 需生成
    @Autowired
    private PwOrderItemMapper orderItemMapper; // 需生成
//    @Autowired
//    private PwPaymentRecordMapper paymentRecordMapper; // 支付记录

    /**
     * 确认下单 (预览)
     */
    public OrderConfirmVo confirmOrder(OrderConfirmDto dto, Long userId) {
        // 1. 查 SKU
        // 注意：你需要去 PwProductSkuMapper 里加一个 selectSkuById 方法，如果只有 selectSkuByProductId 是不够的
        // 这里假设你稍后会补上 selectById
        PwProductSku sku = skuMapper.selectPwProductSkuById(dto.getSkuId());
        if (sku == null) {
            throw new ServiceException("商品规格不存在");
        }

        // 2. 查主商品 (为了获取标题和默认图)
        PwProduct product = productMapper.selectPwProductById(sku.getProductId());
        if (product == null || product.getStatus() != 1) { // 1=上架
            throw new ServiceException("商品已下架或不存在");
        }

        // 3. 校验库存 (只是预览，不扣减，但如果库存不够要提示)
        if (sku.getStock() < dto.getCount()) {
            throw new ServiceException("库存不足，仅剩 " + sku.getStock() + " 件");
        }

        // 4. 组装 VO
        OrderConfirmVo vo = new OrderConfirmVo();
        vo.setProductId(product.getId());
        vo.setSkuId(sku.getId());
        vo.setProductTitle(product.getTitle());
        
        // 图片逻辑：优先 SKU 图，否则主图
        vo.setProductPic(StringUtils.isNotEmpty(sku.getSkuPic()) ? sku.getSkuPic() : product.getPic());
        vo.setSkuName(sku.getSkuName());
        
        vo.setPrice(sku.getPrice());
        vo.setCount(dto.getCount());
        
        // 计算总价
        vo.setTotalPrice(sku.getPrice().multiply(new BigDecimal(dto.getCount())));

        // 5. Mock 默认地址 (因为还没做地址模块)
        // 实际上线前，这里应该调用 UserAddressService.getDefault(userId)
        OrderConfirmVo.AddressVo address = new OrderConfirmVo.AddressVo();
        address.setId(888L);
        address.setName("测试用户");
        address.setMobile("13800138000");
        address.setFullAddress("江苏省南京市浦口区宁六路219号(南京信息工程大学)");
        vo.setAddress(address);

        return vo;
    }

    /**
     * 创建订单 (核心事务)
     */
    @Transactional(rollbackFor = Exception.class)
    public String createOrder(OrderCreateDto dto, Long userId) {
        // 1. 基础校验 (查商品状态)
        PwProductSku sku = skuMapper.selectPwProductSkuById(dto.getSkuId());
        if (sku == null) throw new ServiceException("商品不存在");
        PwProduct product = productMapper.selectPwProductById(sku.getProductId());
        if (product == null || product.getStatus() != 1) throw new ServiceException("商品已下架");

        // 不能买自己的
        if (product.getSellerId().equals(userId)) {
            throw new ServiceException("不能购买自己发布的商品");
        }

        // 2. ★★★ 扣减库存 (CAS 乐观锁) ★★★
        int rows = skuMapper.deductStock(dto.getSkuId(), dto.getCount());
        if (rows == 0) {
            throw new ServiceException("库存不足或手慢了");
        }

        // 3. 生成订单主表数据
        PwOrder order = new PwOrder();
        // 使用 Hutool 或若依的 IdUtils 生成雪花算法ID作为订单号
        String orderNo = cn.hutool.core.util.IdUtil.getSnowflakeNextIdStr();
        order.setOrderNo(orderNo);
        order.setBuyerId(userId);
        order.setSellerId(product.getSellerId());

        // 计算金额
        BigDecimal totalAmount = sku.getPrice().multiply(new BigDecimal(dto.getCount()));
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount); // 暂无优惠券
        order.setStatus(0); // 0=待支付

        // 快照地址 (Mock)
        order.setReceiverName("测试用户");
        order.setReceiverPhone("13800138000");
        order.setReceiverDetailAddress("南京信息工程大学");
        order.setNote(dto.getRemark());
        order.setCreateTime(new Date());

        // 设置过期时间 (15分钟后)
        Date expireTime = new Date(System.currentTimeMillis() + 15 * 60 * 1000);
        order.setExpireTime(expireTime);

        orderMapper.insertPwOrder(order); // 记得生成这个Mapper

        // 4. 生成订单项 (Snapshot 快照)
        PwOrderItem item = new PwOrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(orderNo);
        item.setProductId(product.getId());
        item.setSkuId(sku.getId());
        // ★ 重点：记录购买时的快照信息 (防止卖家改图改价)
        item.setProductName(product.getTitle());
        item.setProductPic(StringUtils.isNotEmpty(sku.getSkuPic()) ? sku.getSkuPic() : product.getPic());
        item.setSkuName(sku.getSkuName());
        item.setProductPrice(sku.getPrice());
        item.setBuyCount(dto.getCount());

        orderItemMapper.insertPwOrderItem(item); // 记得生成这个Mapper

        // 5. TODO: 发送延时消息到 MQ (处理超时未支付自动关单)
        // rabbitTemplate.convertAndSend("order.delay", orderNo);

        return orderNo;
    }


    /**
     * 模拟支付成功 (开发环境专用)
     */
    public void mockPay(String orderNo) {
        // 1. 查订单
        PwOrder order = orderMapper.selectPwOrderByOrderNo(orderNo);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }

        // 2. 幂等性检查 (重要!)
        // 如果状态已经不是 0 (待支付)，说明可能已经付过了，或者已取消
        if (order.getStatus() != 0) {
            // 如果已经是已支付(1)或已发货(2)...，直接返回成功，不要报错
            if (order.getStatus() >= 1 && order.getStatus() <= 3) {
                return;
            }
            throw new ServiceException("订单状态异常，无法支付");
        }

        // 3. 执行状态流转
        PwOrder update = new PwOrder();
        update.setId(order.getId());
        update.setStatus(1); // 1 = 待发货 (已支付)
        update.setPayType(1); // 1 = 支付宝 (模拟)
        update.setPayTime(new Date());
        update.setTradeNo("MOCK_ALIPAY_" + System.currentTimeMillis()); // 模拟流水号

        orderMapper.updatePwOrder(update);

        // 4. (可选) 记录支付流水 pw_payment_record
        // 为了省事，这一步先略过，等接真支付宝再写
    }

    public List<OrderListVo> listOrders(OrderListQuery query, Long userId) {
        // 1. PageHelper 分页 (注意：关联查询的分页有时候会有坑，这里先简单处理)
        PageHelper.startPage(query.getPageNum(), query.getPageSize());

        // 2. 查询
        List<PwOrder> orders = orderMapper.selectOrderList(userId, query.getRole(), query.getStatus());

        // 3. 转换 VO
        return orders.stream().map(order -> {
            OrderListVo vo = new OrderListVo();
            BeanUtil.copyProperties(order, vo);
            if ("buyer".equals(query.getRole())) {
                // 我是买家，对方就是卖家
                vo.setOtherSideUserId(order.getSellerId());
                // 如果要昵称，还得去 userMapper 查一下，或者在 SQL 里多 Join 一次 pw_user 表
                vo.setOtherSideNickname(order.getSellerNickname());
            } else {
                // 我是卖家，对方就是买家
                vo.setOtherSideUserId(order.getBuyerId());
                vo.setOtherSideNickname(order.getBuyerNickname());
            }
            // 这里因为我们用了 MyBatis 关联查询，order.getItemList() 应该已经有值了
            vo.setItemList(order.getItemList());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 确认收货
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(String orderNo, Long userId) {
        // 1. 查订单
        PwOrder order = orderMapper.selectPwOrderByOrderNo(orderNo);
        if (order == null) throw new ServiceException("订单不存在");

        // 2. 鉴权 (只能是买家操作)
        if (!order.getBuyerId().equals(userId)) {
            throw new ServiceException("无权操作");
        }

        // 3. 状态检查
        // 只有 1(待发货) 或 2(已发货) 才能确认收货
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new ServiceException("订单状态不正确");
        }

        // 4. 更新状态 -> 3 (交易成功)
        PwOrder update = new PwOrder();
        update.setId(order.getId());
        update.setStatus(3);
        // update.setFinishTime(new Date()); // 如果表里有这个字段

        orderMapper.updatePwOrder(update);
    }
}