package com.pinwu.app.modules.order.controller;

import com.pinwu.app.modules.auth.domain.model.AppLoginUser;
import com.pinwu.app.modules.auth.service.AppTokenService;
import com.pinwu.app.modules.order.domain.dto.OrderConfirmDto;
import com.pinwu.app.modules.order.domain.dto.OrderCreateDto;
import com.pinwu.app.modules.order.domain.dto.OrderListQuery;
import com.pinwu.app.modules.order.domain.vo.OrderConfirmVo;
import com.pinwu.app.modules.order.domain.vo.OrderListVo;
import com.pinwu.app.modules.order.service.AppOrderService;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/app/order")
public class AppOrderController {

    @Autowired
    private AppOrderService orderService;

    @Autowired
    private AppTokenService tokenService;

    /**
     * 确认下单 (预览)
     * 场景：用户在详情页点击“立即购买”，跳转到确认页时调用
     */
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody @Validated OrderConfirmDto dto, HttpServletRequest request) {
        AppLoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser == null) {
            return AjaxResult.error(401, "请先登录");
        }

        OrderConfirmVo vo = orderService.confirmOrder(dto, loginUser.getPwUser().getId());
        return AjaxResult.success(vo);
    }

    /**
     * 提交订单
     */
    @PostMapping("/create")
    public AjaxResult create(@RequestBody @Validated OrderCreateDto dto, HttpServletRequest request) {
        AppLoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser == null) return AjaxResult.error(401, "请登录");

        String orderNo = orderService.createOrder(dto, loginUser.getPwUser().getId());

        // 返回订单号，前端拿到后跳转收银台
        return AjaxResult.success("下单成功", orderNo);
    }

    /**
     * 模拟支付
     */
    @PostMapping("/pay/mock")
    public AjaxResult mockPay(@RequestParam String orderNo) {
        // 实际项目中，这里应该先校验当前用户是不是订单的主人
        // 但为了方便测试，先不做严格校验
        orderService.mockPay(orderNo);
        return AjaxResult.success("支付成功");
    }

    /**
     * 订单列表 (买家/卖家通用)
     */
    @GetMapping("/list")
    public AjaxResult list(OrderListQuery query, HttpServletRequest request) {
        AppLoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser == null) return AjaxResult.error(401, "请登录");

        List<OrderListVo> list = orderService.listOrders(query, loginUser.getPwUser().getId());

        // 若依的 TableDataInfo 分页包装 (可选)
        // return getDataTable(list);

        return AjaxResult.success(list);
    }

    /**
     * 确认收货
     */
    @PostMapping("/confirm-receive")
    public AjaxResult confirmReceive(@RequestParam String orderNo, HttpServletRequest request) {
        AppLoginUser loginUser = tokenService.getLoginUser(request);
        orderService.confirmReceive(orderNo, loginUser.getPwUser().getId());
        return AjaxResult.success("交易完成");
    }
}