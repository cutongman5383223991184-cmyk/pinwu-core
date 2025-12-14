package com.pinwu.app.modules.order.domain.dto;

import lombok.Data;

@Data
public class OrderListQuery {
    /**
     * 页码
     */
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /**
     * 角色视图:
     * "buyer" - 我买的 (默认)
     * "seller" - 我卖的
     */
    private String role = "buyer";

    /**
     * 订单状态:
     * null - 全部
     * 0 - 待付款
     * 1 - 待发货
     * 2 - 已发货
     * 3 - 交易完成
     */
    private Integer status;
}