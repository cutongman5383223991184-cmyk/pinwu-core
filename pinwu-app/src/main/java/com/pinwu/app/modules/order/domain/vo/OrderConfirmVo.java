package com.pinwu.app.modules.order.domain.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderConfirmVo {
    
    // --- 1. 商品快照信息 ---
    private Long productId;
    private Long skuId;
    
    private String productTitle;
    private String productPic; // 优先用 SKU 图，没有则用主图
    private String skuName;    // "红色, 128G"
    
    private BigDecimal price;  // 单价
    private Integer count;     // 数量
    
    // --- 2. 金额计算 ---
    private BigDecimal totalPrice; // 总价 = price * count (未来可减去优惠券)
    
    // --- 3. 收货地址 (Mock) ---
    // 真实项目中应该去查 pw_user_address 表
    private AddressVo address;

    @Data
    public static class AddressVo {
        private Long id;
        private String name;
        private String mobile;
        private String fullAddress;
    }
}