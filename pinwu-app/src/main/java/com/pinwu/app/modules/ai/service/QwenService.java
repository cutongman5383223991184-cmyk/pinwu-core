//package com.pinwu.app.modules.ai.service;
//
//import cn.hutool.core.util.StrUtil;
//import cn.hutool.http.HttpRequest;
//import cn.hutool.http.HttpResponse;
//import cn.hutool.json.JSONArray;
//import cn.hutool.json.JSONObject;
//import cn.hutool.json.JSONUtil;
//import com.pinwu.app.modules.ai.domain.vo.AiResultVo;
//import com.pinwu.common.exception.ServiceException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
///**
// * 通义千问 AI 服务 (HTTP 直连版)
// * 抛弃 SDK，直接发送 HTTP 请求，彻底解决 JDK8 下的版本兼容问题
// */
//@Service
//public class QwenService {
//
//    private static final Logger log = LoggerFactory.getLogger(QwenService.class);
//
//    @Value("${pinwu.ai.apiKey}")
//    private String apiKey;
//
//    // 阿里云 DashScope 原生 API 地址
//    private static final String URL_GENERATION = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
//    private static final String URL_TEXT_GEN = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
//
//    // 提示词模板 (Prompt Engineering)
//    private static final String FORMAT_INSTRUCTION =
//            "请严格只返回一个合法的 JSON 对象，不要包含Markdown代码块(```json)，不要包含其他废话。\n" +
//                    "JSON格式要求：\n" +
//                    "{\n" +
//                    "  \"title\": \"商品标题(品牌型号+成色)\",\n" +
//                    "  \"category\": \"推荐分类(如:数码/手机)\",\n" +
//                    "  \"tags\": [\"标签1\", \"标签2\"],\n" +
//                    "  \"description\": \"转手文案\"\n" +
//                    "}";
//
//    /**
//     * 场景 1: 图片转文字 (Qwen-VL)
//     */
//    public AiResultVo analyzeImage(String imageUrl) {
//        // 1. 拼装请求体 (看图模式)
//        JSONObject input = new JSONObject();
//        JSONArray messages = new JSONArray();
//
//        JSONObject message = new JSONObject();
//        message.set("role", "user");
//
//        JSONArray content = new JSONArray();
//        // 放图片
//        content.add(new JSONObject().set("image", imageUrl));
//        // 放提示词
//        content.add(new JSONObject().set("text", "请分析图中商品。\n" + FORMAT_INSTRUCTION));
//
//        message.set("content", content);
//        messages.add(message);
//
//        input.set("messages", messages);
//
//        JSONObject body = new JSONObject();
//        body.set("model", "qwen-vl-plus"); // 使用 VL 模型
//        body.set("input", input);
//        // 必须设置这个参数，否则 VL 模型可能返回空
//        body.set("parameters", new JSONObject().set("result_format", "message"));
//
//        return callAiApi(URL_GENERATION, body);
//    }
//
//    /**
//     * 场景 2: 文本生成 (Qwen-Turbo)
//     */
//    public AiResultVo generateByKeywords(String keywords) {
//        // 1. 拼装请求体 (纯文本模式)
//        JSONObject input = new JSONObject();
//        JSONArray messages = new JSONArray();
//
//        // System 提示
//        messages.add(new JSONObject().set("role", "system").set("content", "你是一个二手交易助手。"));
//        // User 提示
//        messages.add(new JSONObject().set("role", "user").set("content", "关键词：" + keywords + "\n" + FORMAT_INSTRUCTION));
//
//        input.set("messages", messages);
//
//        JSONObject body = new JSONObject();
//        body.set("model", "qwen-turbo"); // 使用便宜的文本模型
//        body.set("input", input);
//
//        return callAiApi(URL_TEXT_GEN, body);
//    }
//
//    /**
//     * 统一发送 HTTP 请求
//     */
//    private AiResultVo callAiApi(String url, JSONObject body) {
//        try {
//            log.info("正在请求AI接口...");
//
//            HttpResponse response = HttpRequest.post(url)
//                    .header("Authorization", "Bearer " + apiKey)
//                    .header("Content-Type", "application/json")
//                    .body(body.toString())
//                    .timeout(60000)
//                    .execute();
//
//            String resBody = response.body();
//            log.info("AI 响应: {}", resBody);
//
//            JSONObject jsonRes = JSONUtil.parseObj(resBody);
//
//            // 检查错误
//            if (jsonRes.containsKey("code") && jsonRes.getStr("code") != null) {
//                throw new ServiceException("AI 调用失败: " + jsonRes.getStr("message"));
//            }
//
//            if (!response.isOk()) {
//                throw new ServiceException("AI 接口 HTTP 错误: " + response.getStatus());
//            }
//
//            // 提取文本
//            String aiText = extractContent(jsonRes);
//            log.info("提取的AI文本: {}", aiText);
//
//            return parseAiJson(aiText);
//
//        } catch (ServiceException se) {
//            throw se;
//        } catch (Exception e) {
//            log.error("AI 服务异常", e);
//            throw new ServiceException("AI 服务暂时不可用: " + e.getMessage());
//        }
//    }
//
//    /**
//     * ★★★ 核心修复：兼容两种返回格式 ★★★
//     */
//    private String extractContent(JSONObject jsonRes) {
//        try {
//            JSONObject output = jsonRes.getJSONObject("output");
//
//            // 1. 优先检查 output.text (qwen-turbo 文本模型)
//            if (output.containsKey("text")) {
//                log.debug("使用 output.text 格式解析");
//                return output.getStr("text");
//            }
//
//            // 2. 再检查 output.choices (qwen-vl-plus 视觉模型)
//            if (output.containsKey("choices")) {
//                log.debug("使用 output.choices 格式解析");
//                JSONArray choices = output.getJSONArray("choices");
//                if (choices != null && !choices.isEmpty()) {
//                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
//                    Object contentObj = msg.get("content");
//
//                    if (contentObj instanceof JSONArray) {
//                        StringBuilder sb = new StringBuilder();
//                        JSONArray contentArr = (JSONArray) contentObj;
//                        for (int i = 0; i < contentArr.size(); i++) {
//                            JSONObject item = contentArr.getJSONObject(i);
//                            if (item.containsKey("text")) {
//                                sb.append(item.getStr("text"));
//                            }
//                        }
//                        return sb.toString();
//                    } else {
//                        return contentObj.toString();
//                    }
//                }
//            }
//
//            log.warn("未能识别的响应格式: {}", output);
//            return "";
//
//        } catch (Exception e) {
//            log.error("解析响应内容失败", e);
//            return "";
//        }
//    }
//
//    private AiResultVo parseAiJson(String rawAiText) {
//        if (StrUtil.isEmpty(rawAiText)) return new AiResultVo();
//
//        // ★★★ 核心：清洗 Markdown 标记 ★★★
//        String cleanJson = rawAiText.replace("```json", "")
//                .replace("```", "")
//                .trim();
//
//        try {
//            return JSONUtil.toBean(cleanJson, AiResultVo.class);
//        } catch (Exception e) {
//            log.warn("JSON 解析失败，降级处理");
//            AiResultVo fallback = new AiResultVo();
//            fallback.setDescription(cleanJson);
//            fallback.setTitle("AI 生成结果");
//            return fallback;
//        }
//    }
//}