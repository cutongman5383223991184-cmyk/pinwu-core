package com.pinwu.app.modules.product.mapper;

import com.pinwu.app.modules.product.domain.entity.PwProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PwProductSkuMapper {
    /**
     * 批量插入 SKU (核心)
     */
    int insertBatch(List<PwProductSku> skuList);

    List<PwProductSku> selectSkuByProductId(Long productId);

    PwProductSku selectPwProductSkuById(Long skuId);

    /**
     * 扣减库存 (乐观锁)
     * @param skuId 规格ID
     * @param count 扣减数量
     * @return 影响行数 (如果返回0，说明库存不足或并发冲突)
     */
    int deductStock(@Param("skuId") Long skuId, @Param("count") Integer count);
}