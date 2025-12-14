package com.pinwu.app.modules.order.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinwu.common.core.domain.BaseEntity;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class PwOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;     // 订单号

    private Long buyerId;       // 买家ID
    private Long sellerId;      // 卖家ID

    private BigDecimal totalAmount; // 订单总额
    private BigDecimal payAmount;   // 实付金额
    private Integer payType;        // 支付方式

    private Integer status;         // 0-待付款

    // 收货地址快照
    private String receiverName;
    private String receiverPhone;
    private String receiverProvince;
    private String receiverCity;
    private String receiverRegion;
    private String receiverDetailAddress;

    private String note; // ★ 对应 DTO 里的 remark

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime; // 过期时间
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;
    
    private String tradeNo;  // 支付宝流水号

    private String buyerNickname;
    private String sellerNickname;

    /** * 订单项列表 (非数据库字段，仅用于MyBatis关联查询)
     */
    private List<PwOrderItem> itemList;
}