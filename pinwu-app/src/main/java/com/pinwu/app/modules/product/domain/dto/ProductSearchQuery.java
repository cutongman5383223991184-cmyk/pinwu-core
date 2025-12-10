package com.pinwu.app.modules.product.domain.dto;

import lombok.Data;

@Data
public class ProductSearchQuery {
    private String keyword; // 搜索词 (iPhone)
    
    private Double latitude;  // 用户当前纬度
    private Double longitude; // 用户当前经度
    
    /**
     * 排序方式:
     * 0 - 综合排序 (默认: 距离+热度+文本)
     * 1 - 价格最低
     * 2 - 最新发布
     */
    private Integer sort = 0; 
    
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}