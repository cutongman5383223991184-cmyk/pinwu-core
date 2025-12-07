package com.pinwu.app.modules.ai.controller;

import com.pinwu.app.modules.ai.domain.vo.AiResultVo;
import com.pinwu.app.modules.ai.service.AppAiService;
import com.pinwu.app.modules.auth.domain.model.AppLoginUser;
import com.pinwu.app.modules.auth.service.AppTokenService;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * C端 AI 智能辅助接口
 * 核心亮点：分级限流 + 降级策略
 */
@RestController
@RequestMapping("/app/ai")
public class AppAiController {

    @Autowired
    private AppAiService appAiService; // 注入新的 Service

    @Autowired
    private AppTokenService appTokenService;

    // 1. 识图 (POST)
    @PostMapping("/analyze-image")
    public AjaxResult analyzeImage(@RequestBody Map<String,String> params, HttpServletRequest request) {
        String imageUrl = params.get("imageUrl");  // ✅ 从 Map 中取出 URL
        AppLoginUser loginUser = appTokenService.getLoginUser(request);
        // 这里把 loginUser 传进去，里面包含了 userId 和 vipExpireTime
        AiResultVo result = appAiService.analyzeImage(imageUrl, loginUser);
        return AjaxResult.success(result);
    }

    // 2. 文本生成 (POST)
    @PostMapping("/generate-text")
    public AjaxResult generateText(@RequestBody Map<String,String> params, HttpServletRequest request) {
        String keywords = params.get("keywords");  // ✅ 从 Map 中取出关键词
        AppLoginUser loginUser = appTokenService.getLoginUser(request);
        AiResultVo result = appAiService.generateByKeywords(keywords, loginUser);
        return AjaxResult.success(result);
    }
}