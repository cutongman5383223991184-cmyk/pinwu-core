package com.pinwu.app.modules.product.domain.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 对应索引: pinwu_product
 * createIndex = true 表示启动时自动建索引 (生产环境通常关掉手动建，但开发环境开启很方便)
 */
@Data
@Document(indexName = "pinwu_product", createIndex = true)
public class ProductDoc {

    @Id
    private Long id;

    /** * 标题: 核心搜索字段
     * analyzer = "ik_max_word": 存索引时，把 "南京信息工程大学" 拆得越细越好
     * searchAnalyzer = "ik_smart": 用户搜时，把 "南京大学" 拆得聪明点
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword, index = false) // 不参与搜索，只展示
    private String pic;

    @Field(type = FieldType.Keyword) // 标签整体匹配，不分词
    private List<String> tags;

    // --- 区域筛选字段 (Keyword类型精确匹配) ---
    @Field(type = FieldType.Keyword)
    private String province;
    @Field(type = FieldType.Keyword)
    private String city;
    @Field(type = FieldType.Keyword)
    private String region;

    /** LBS 坐标点 */
    @GeoPointField
    private GeoPoint location;

    /** 状态: 1上架 */
    @Field(type = FieldType.Integer)
    private Integer status;

    /** 浏览量 (用于算分) */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Date)
    private Date createTime;
}