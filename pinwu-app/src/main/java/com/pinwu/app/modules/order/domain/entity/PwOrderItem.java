package com.pinwu.app.modules.order.domain.entity;


import com.pinwu.common.core.domain.BaseEntity;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PwOrderItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private String orderNo;

    private Long productId;
    private Long skuId;

    // 快照信息
    private String productName;
    private String productPic;
    private String skuName;
    private BigDecimal productPrice;
    private Integer buyCount;
}