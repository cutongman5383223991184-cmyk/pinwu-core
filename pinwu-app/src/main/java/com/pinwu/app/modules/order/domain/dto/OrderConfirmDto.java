package com.pinwu.app.modules.order.domain.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class OrderConfirmDto {
    /** 必须选择一个具体的规格 (哪怕是单品，我们之前也生成了默认SKU) */
    @NotNull(message = "商品规格不能为空")
    private Long skuId;

    /** 购买数量 */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "至少购买1件")
    private Integer count;
}