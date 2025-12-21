package com.pinwu.app.modules.ai.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pinwu.app.modules.ai.domain.vo.AiResultVo;
import com.pinwu.common.core.domain.model.AppLoginUser;
import com.pinwu.common.core.domain.entity.PwUser;
import com.pinwu.common.core.redis.RedisCache;
import com.pinwu.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * AI 智能服务 (企业级限流版)
 *
 * 核心特性：
 * 1. 双层限流保护：用户级别限流 + 系统级别 QPM 限流
 * 2. VIP 权限区分：普通用户/会员用户不同配额
 * 3. 降级策略：Redis 故障时的兜底处理
 * 4. 监控告警：资源使用率预警
 *
 * @author pinwu
 */
@Service
public class AppAiService {

    private static final Logger log = LoggerFactory.getLogger(AppAiService.class);

    @Value("${pinwu.ai.apiKey}")
    private String apiKey;

    @Autowired
    private RedisCache redisCache;

    // ==================== 限流配置 ====================

    // 用户每日限额
    private static final int LIMIT_IMG_NORMAL = 1;    // 普通用户：图片 1次/天
    private static final int LIMIT_IMG_VIP = 20;      // 会员用户：图片 20次/天
    private static final int LIMIT_TEXT_NORMAL = 3;   // 普通用户：文本 3次/天
    private static final int LIMIT_TEXT_VIP = 50;     // 会员用户：文本 50次/天

    // 全局 QPM 限制（保护 API Key 不被封禁）
    private static final int LIMIT_GLOBAL_QPM = 60;   // 全站每分钟最多 60 次
    private static final int QPM_WARN_THRESHOLD = 80; // QPM 使用率告警阈值（百分比）

    // Redis Key 前缀
    private static final String REDIS_USER_LIMIT_KEY = "ai:limit:user:";
    private static final String REDIS_GLOBAL_QPM_KEY = "ai:limit:global:qpm:";

    // 阿里云 API 地址
    private static final String URL_VL_GENERATION = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static final String URL_TEXT_GENERATION = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    // 提示词模板
    private static final String FORMAT_INSTRUCTION =
            "请严格只返回一个合法的 JSON 对象，不要包含Markdown代码块(```json)，不要包含其他废话。\n" +
                    "JSON格式要求：\n" +
                    "{\n" +
                    "  \"title\": \"商品标题(品牌型号+成色)\",\n" +
                    "  \"category\": \"推荐分类(如:数码/手机)\",\n" +
                    "  \"tags\": [\"标签1\", \"标签2\"],\n" +
                    "  \"description\": \"转手文案\"\n" +
                    "}";

    // ==================== 对外接口 ====================

    /**
     * 图片分析 (Qwen-VL)
     *
     * @param imageUrl   图片URL
     * @param loginUser  当前登录用户
     * @return AI 分析结果
     */
    public AiResultVo analyzeImage(String imageUrl, AppLoginUser loginUser) {
        // 第一层：系统级保护（QPM 限流）
        checkGlobalQpm();

        // 第二层：用户级保护（每日配额）
        checkAndDeductUserQuota(loginUser, "image");

        // 构造请求
        JSONObject body = buildVisionRequest(imageUrl);

        return callAiApi(URL_VL_GENERATION, body);
    }

    /**
     * 文本生成 (Qwen-Turbo)
     *
     * @param keywords   用户输入的关键词
     * @param loginUser  当前登录用户
     * @return AI 生成结果
     */
    public AiResultVo generateByKeywords(String keywords, AppLoginUser loginUser) {
        // 第一层：系统级保护
        checkGlobalQpm();

        // 第二层：用户级保护
        checkAndDeductUserQuota(loginUser, "text");

        // 构造请求
        JSONObject body = buildTextRequest(keywords);

        return callAiApi(URL_TEXT_GENERATION, body);
    }

    // ==================== 核心：双层限流机制 ====================

    /**
     * 【第一层】全局 QPM 限流
     *
     * 作用：保护上游 API，防止被封禁
     * 原理：Redis increment 原子计数，Key 精确到分钟
     * 降级：Redis 异常时放行，避免影响正常用户
     */
    private void checkGlobalQpm() {
        try {
            // Key 格式: ai:limit:global:qpm:202512071430
            String currentMinute = DateUtil.format(new Date(), "yyyyMMddHHmm");
            String key = REDIS_GLOBAL_QPM_KEY + currentMinute;

            // 原子递增，不存在则创建并返回 1
            Long currentQpm = redisCache.redisTemplate.opsForValue().increment(key);

            // 首次访问设置过期时间（5分钟后自动清理）
            if (currentQpm != null && currentQpm == 1) {
                redisCache.expire(key, 5, TimeUnit.MINUTES);
            }

            // 使用率告警（提前预警，便于运维）
            if (currentQpm != null) {
                int usagePercent = (int) (currentQpm * 100 / LIMIT_GLOBAL_QPM);
                if (usagePercent >= QPM_WARN_THRESHOLD && usagePercent < 100) {
                    log.warn("【QPM告警】当前使用率 {}%，已用 {}/{}，请关注",
                            usagePercent, currentQpm, LIMIT_GLOBAL_QPM);
                }
            }

            // 超限熔断
            if (currentQpm != null && currentQpm > LIMIT_GLOBAL_QPM) {
                log.warn("【QPM熔断】触发全局限流，当前 QPM: {}", currentQpm);
                throw new ServiceException("AI 助手正在排队中，请稍后再试~");
            }

        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            // Redis 异常时降级放行，保证可用性优先
            log.error("【QPM降级】Redis 异常，降级放行", e);
        }
    }

    /**
     * 【第二层】用户配额限流
     *
     * 作用：控制用户使用成本，区分 VIP 权益
     * 原理：按用户+日期+类型维度计数
     */
    private void checkAndDeductUserQuota(AppLoginUser loginUser, String type) {
        PwUser user = loginUser.getPwUser();
        Long userId = user.getId();
        boolean isVip = checkIsVip(user);

        // 确定用户限额
        int limit = getLimit(type, isVip);

        // 构造 Key: ai:limit:user:image:1:20251207
        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String key = REDIS_USER_LIMIT_KEY + type + ":" + userId + ":" + date;

        try {
            // 原子递增
            Long currentCount = redisCache.redisTemplate.opsForValue().increment(key);

            // 首次访问设置过期时间
            if (currentCount != null && currentCount == 1) {
                redisCache.expire(key, 1, TimeUnit.DAYS);
            }

            // 超额判断
            if (currentCount != null && currentCount > limit) {
                String userType = isVip ? "尊贵的会员" : "普通用户";
                String actionType = "image".equals(type) ? "识图" : "文案生成";
                String upgradeHint = isVip ? "" : " 开通会员可享更多次数！";

                throw new ServiceException(
                        String.format("%s您好，今日%s次数已用完(%d次)。%s",
                                userType, actionType, limit, upgradeHint)
                );
            }

        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("【用户限流降级】Redis 异常，降级放行", e);
        }
    }

    /**
     * 获取用户限额
     */
    private int getLimit(String type, boolean isVip) {
        if ("image".equals(type)) {
            return isVip ? LIMIT_IMG_VIP : LIMIT_IMG_NORMAL;
        } else {
            return isVip ? LIMIT_TEXT_VIP : LIMIT_TEXT_NORMAL;
        }
    }

    /**
     * 判断用户是否是 VIP
     */
    private boolean checkIsVip(PwUser user) {
        Date expireTime = user.getVipExpireTime();
        return expireTime != null && expireTime.after(new Date());
    }

    // ==================== 请求构建 ====================

    /**
     * 构建视觉分析请求
     */
    private JSONObject buildVisionRequest(String imageUrl) {
        JSONArray content = new JSONArray();
        content.add(new JSONObject()
                .set("type", "image")
                .set("image", imageUrl));
        content.add(new JSONObject().set("text", "请分析图中商品。\n" + FORMAT_INSTRUCTION));

        JSONObject message = new JSONObject()
                .set("role", "user")
                .set("content", content);

        JSONArray messages = new JSONArray();
        messages.add(message);

        JSONObject input = new JSONObject()
                .set("messages", messages);

        return new JSONObject()
                .set("model", "qwen-vl-plus")
                .set("input", input)
                .set("parameters", new JSONObject().set("result_format", "message"));
    }

    /**
     * 构建文本生成请求
     */
    private JSONObject buildTextRequest(String keywords) {
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject()
                .set("role", "system")
                .set("content", "你是一个二手交易助手。"));
        messages.add(new JSONObject()
                .set("role", "user")
                .set("content", "关键词：" + keywords + "\n" + FORMAT_INSTRUCTION));

        JSONObject input = new JSONObject().set("messages", messages);

        return new JSONObject()
                .set("model", "qwen-turbo")
                .set("input", input);
    }

    // ==================== HTTP 调用 ====================

    /**
     * 调用阿里云 AI API
     */
    private AiResultVo callAiApi(String url, JSONObject body) {
        try {
            log.info("AI Request: {}", body.toString());

            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();

            if (!response.isOk()) {
                log.error("AI API 响应异常: status={}, body={}", response.getStatus(), response.body());
                throw new ServiceException("AI 服务暂时不可用，请稍后重试");
            }

            JSONObject jsonRes = JSONUtil.parseObj(response.body());

            // 阿里云错误码处理
            if (jsonRes.containsKey("code") && jsonRes.getStr("code") != null) {
                log.error("AI API 返回错误: {}", jsonRes);
                throw new ServiceException("AI 服务异常：" + jsonRes.getStr("message"));
            }

            String aiText = extractContent(jsonRes);
            return parseAiJson(aiText);

        } catch (ServiceException se) {
            throw se;
        } catch (Exception e) {
            log.error("AI Service Error", e);
            throw new ServiceException("AI 服务繁忙，请稍后重试");
        }
    }

    /**
     * 提取 AI 响应内容
     */
    private String extractContent(JSONObject jsonRes) {
        try {
            JSONObject output = jsonRes.getJSONObject("output");

            // 文本模型直接返回
            if (output.containsKey("text")) {
                return output.getStr("text");
            }

            // 视觉模型从 choices 中提取
            if (output.containsKey("choices")) {
                JSONArray choices = output.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                    Object contentObj = msg.get("content");

                    if (contentObj instanceof JSONArray) {
                        StringBuilder sb = new StringBuilder();
                        JSONArray contentArr = (JSONArray) contentObj;
                        for (int i = 0; i < contentArr.size(); i++) {
                            JSONObject item = contentArr.getJSONObject(i);
                            if (item.containsKey("text")) {
                                sb.append(item.getStr("text"));
                            }
                        }
                        return sb.toString();
                    }
                    return contentObj.toString();
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("解析 AI 响应失败", e);
            return "";
        }
    }

    /**
     * 解析 AI 返回的 JSON
     */
    private AiResultVo parseAiJson(String rawText) {
        if (StrUtil.isEmpty(rawText)) {
            return new AiResultVo();
        }

        // 清理 Markdown 代码块标记
        String cleanJson = rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        try {
            return JSONUtil.toBean(cleanJson, AiResultVo.class);
        } catch (Exception e) {
            log.warn("JSON 解析失败，返回原文: {}", cleanJson);
            AiResultVo fallback = new AiResultVo();
            fallback.setDescription(cleanJson);
            fallback.setTitle("AI生成结果");
            return fallback;
        }
    }
}