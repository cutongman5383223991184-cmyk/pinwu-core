package com.pinwu.app.modules.product.mapper;

import com.pinwu.app.modules.product.domain.entity.PwProductSku;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PwProductSkuMapper {
    /**
     * 批量插入 SKU (核心)
     */
    int insertBatch(List<PwProductSku> skuList);

    List<PwProductSku> selectSkuByProductId(Long productId);
}