package com.pinwu.app.modules.order.domain.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class OrderCreateDto {
    @NotNull(message = "规格ID不能为空")
    private Long skuId;

    @Min(value = 1, message = "数量至少为1")
    private Integer count;

    @NotNull(message = "收货地址不能为空")
    private Long addressId; // 虽然我们现在是Mock地址，但流程上要有这个ID

    private String remark; // 买家留言
    
    // 面试加分项：防重令牌 (Token)，防止用户手抖点两次
    // private String orderToken; 
}