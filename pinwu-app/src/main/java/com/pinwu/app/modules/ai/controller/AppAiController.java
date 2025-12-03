package com.pinwu.app.modules.ai.controller;

import com.pinwu.app.modules.ai.domain.vo.AiResultVo;
import com.pinwu.app.modules.ai.service.QwenService;
import com.pinwu.common.annotation.Anonymous;
import com.pinwu.common.annotation.RateLimiter;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * C端 AI 智能辅助接口
 * 核心亮点：分级限流 + 降级策略
 */
@RestController
@RequestMapping("/app/ai")
public class AppAiController {

    @Autowired
    private QwenService qwenService;

    /**
     * 场景 A: 图片识图 (每天限 1 次)
     * 接口地址: POST /app/ai/analyze-image
     */
    @Anonymous // ★ 暂时允许匿名访问，方便你调试。后续做完登录模块后删掉！
    @PostMapping("/analyze-image")
    // 限流：key=ai_image, 24小时(86400秒)内只能调 1 次
    @RateLimiter(key = "ai_image", time = 86400, count = 1)
    public AjaxResult analyzeImage(@RequestBody Map<String, String> body) {
        String imageUrl = body.get("imageUrl");
        AiResultVo result = qwenService.analyzeImage(imageUrl);
        return AjaxResult.success(result);
    }

    /**
     * 场景 B: 文本润色 (每天限 3 次)
     * 接口地址: POST /app/ai/generate-text
     */
    @Anonymous // ★ 暂时允许匿名访问
    @PostMapping("/generate-text")
    // 限流：key=ai_text, 24小时内只能调 3 次
    @RateLimiter(key = "ai_text", time = 86400, count = 3)
    public AjaxResult generateText(@RequestBody Map<String, String> body) {
        String keywords = body.get("keywords");
        AiResultVo result = qwenService.generateByKeywords(keywords);
        return AjaxResult.success(result);
    }
}