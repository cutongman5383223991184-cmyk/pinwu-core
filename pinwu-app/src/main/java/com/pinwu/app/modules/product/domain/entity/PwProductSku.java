package com.pinwu.app.modules.product.domain.entity;

import com.pinwu.common.core.domain.BaseEntity;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PwProductSku extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId; // 关联的主表ID

    private String skuName; // 规格名 (如: "高等数学", "红色")
    private String skuPic;  // 规格图
    private BigDecimal price;
    private Integer stock;
    
    private Integer version; // 乐观锁版本号
}