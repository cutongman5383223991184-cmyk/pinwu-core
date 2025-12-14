package com.pinwu.app.modules.order.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinwu.app.modules.order.domain.entity.PwOrderItem;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderListVo {
    private Long id;
    private String orderNo;
    
    // 金额与状态
    private BigDecimal totalAmount; // 订单总额
    private Integer status;         // 状态
    
    // 谁买的/谁卖的 (视查询视角而定)
    private Long otherSideUserId;   // 对方ID
    private String otherSideNickname; // 对方昵称 (可选，需关联查询)

    // ★★★ 核心：订单里的商品快照 ★★★
    // 列表页通常只展示第一个商品，或者展示全部 Item
    private List<PwOrderItem> itemList;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}