package com.pinwu.app.modules.auth.controller;

import com.pinwu.app.modules.auth.domain.vo.AppLoginBody;
import com.pinwu.app.modules.auth.service.AppAuthService;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    @Autowired
    private AppAuthService appAuthService;

    /**
     * 1. 发送验证码
     * 直接返回 code 给前端，前端填入输入框，实现 Mock 效果
     */
    @PostMapping("/sendCode")
    public AjaxResult sendCode(@RequestParam String mobile) {
        String code = appAuthService.sendSmsCode(mobile);
        return AjaxResult.success("发送成功", code);
    }

    /**
     * 2. 登录接口
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody AppLoginBody loginBody) {
        String token = appAuthService.login(loginBody);
        return AjaxResult.success().put("token", token);
    }
}