package com.pinwu.app.modules.product.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinwu.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 二手商品对象 pw_product
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PwProduct extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 卖家ID */
    private Long sellerId;

    /** 分类ID */
    private Long categoryId;

    /** 商品标题 */
    private String title;

    /** 商品详情描述 */
    private String detail;

    /** 主图URL */
    private String pic;

    /** 画廊图片(逗号分隔) */
    private String albumPics;

    /** AI生成的标签数组 (存JSON字符串) */
    private String tags;

    /** 展示价格 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    // --- LBS 中文信息 ---
    /** 省份 */
    private String province;
    /** 城市 */
    private String city;
    /** 区/县 */
    private String region;
    /** 定位地点名 */
    private String locationName;
    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;

    // --- 联系方式 ---
    /** 联系方式: 0-手机, 1-微信 */
    private Integer contactType;
    /** 联系号码 */
    private String contactValue;

    // --- 状态与统计 ---
    /** 浏览量 */
    private Integer viewCount;
    /** 点赞数 */
    private Integer likeCount;
    /** 状态: 0-下架, 1-上架, 2-已售 */
    private Integer status;

    /** 审核状态 */
    private String auditStatus;
    /** 驳回原因 */
    private String auditMsg;

    /** 删除标志 */
    private String delFlag;
}