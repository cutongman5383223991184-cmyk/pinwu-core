package com.pinwu.app.modules.auth.controller;

import com.pinwu.common.core.domain.model.AppLoginUser;
import com.pinwu.app.modules.auth.domain.vo.AppLoginBody;
import com.pinwu.app.modules.auth.service.AppAuthService;
import com.pinwu.framework.web.service.AppTokenService;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/auth")
public class AppAuthController {

    @Autowired
    private AppAuthService appAuthService;

    @Autowired
    private AppTokenService tokenService;

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

    /**
     * 3. 获取个人信息 (APP端专用)
     * 前端 header 带着 token 来调这个接口
     */
    @GetMapping("/getProfile")
    public AjaxResult getProfile(javax.servlet.http.HttpServletRequest request) {
        // 1. 解析 Token
        AppLoginUser loginUser = tokenService.getLoginUser(request);

        if (loginUser == null) {
            return AjaxResult.error(401, "Token已过期或无效");
        }

        // 2. 返回用户信息 (PwUser)
        // loginUser.getPwUser() 里包含了 nickname, avatar, mobile 等信息
        return AjaxResult.success(loginUser.getPwUser());
    }
}