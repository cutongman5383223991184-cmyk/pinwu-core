package com.pinwu.app.modules.product.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinwu.app.modules.product.domain.entity.PwProductSku;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ProductDetailVo {

    // --- 1. SPU 主商品信息 ---
    private Long id;
    private String title;
    private String detail;
    private String pic;
    private String albumPics;
    private List<String> tags; // 转为 List 方便前端
    private BigDecimal price;
    private BigDecimal originalPrice;
    
    // LBS 信息
    private String province;
    private String city;
    private String region;
    private String locationName;
    
    // 状态
    private Integer viewCount;
    private Integer likeCount;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    // --- 2. SKU 规格列表 (如: 高数, 英语) ---
    private List<PwProductSku> skuList;

    // --- 3. 卖家信息 (脱敏版) ---
    private SellerVo seller;

    /**
     * 内部类：卖家信息 VO
     */
    @Data
    public static class SellerVo {
        private Long userId;
        private String nickname;
        private String avatar;
        // 这是一个大厂考点：返回“多久前活跃”，而不是具体的“最后登录时间”
        private String activeTimeText; // 如 "刚刚活跃", "3小时前"
        
        // 联系方式 (脱敏后)
        private Integer contactType;
        private String contactValue; // "138****1234"
    }
}