package com.pinwu.app.modules.product.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductSkuDto {
    private String skuName; // 规格名称 (如: "高数上册")
    private BigDecimal price; // 价格
    private Integer stock;    // 库存
    private String skuPic;    // 规格图 (可选)
}