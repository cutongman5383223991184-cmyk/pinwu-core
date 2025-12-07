package com.pinwu.app.modules.ai.domain.vo;

import java.util.List;

/**
 * AI 分析结果视图对象
 */
public class AiResultVo {
    /** 商品标题 (如：99新 iPhone 13 国行) */
    private String title;

    /** 推荐分类 (如：数码/手机) */
    private String category;

    /** 标签列表 (如：[无划痕, 电池健康90%, 面交]) */
    private List<String> tags;

    /** 商品描述文案 */
    private String description;

    // --- Getter & Setter ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static AiResultVo mock() {
        AiResultVo vo = new AiResultVo();
        vo.setTitle("测试商品 - " + System.currentTimeMillis());
        vo.setCategory("测试分类");
        return vo;
    }
}