package com.pinwu.app.modules.product.domain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductPublishDto {

    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 30, message = "标题长度需在5-30字之间")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String detail;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "原价不能为空")
    private BigDecimal originalPrice;

    @NotBlank(message = "主图不能为空")
    private String pic;

    private String albumPics; // 画廊图片，逗号分隔

    private List<String> tags; // AI 生成的标签

    // --- LBS 信息 (直接存中文) ---
    @NotBlank(message = "省份不能为空")
    private String province;
    @NotBlank(message = "城市不能为空")
    private String city;
    @NotBlank(message = "区县不能为空")
    private String region;
    
    private String locationName; // 具体位置

    @NotNull(message = "纬度不能为空")
    private Double latitude;
    @NotNull(message = "经度不能为空")
    private Double longitude;

    private Integer contactType; // 0-手机 1-微信
    private String contactValue;
}