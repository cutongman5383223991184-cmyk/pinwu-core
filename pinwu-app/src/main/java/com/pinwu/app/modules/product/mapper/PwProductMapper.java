package com.pinwu.app.modules.product.mapper;

import com.pinwu.app.modules.product.domain.entity.PwProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PwProductMapper {

    /**
     * 查询商品
     */
    PwProduct selectPwProductById(Long id);

    /**
     * 查询商品列表
     */
    List<PwProduct> selectPwProductList(PwProduct pwProduct);

    /**
     * 新增商品
     * ★★★ 重点：Service层需要立刻拿到ID去写ES，所以XML里必须配置回填ID
     */
    int insertPwProduct(PwProduct pwProduct);

    /**
     * 修改商品
     */
    int updatePwProduct(PwProduct pwProduct);

    /**
     * 批量删除商品
     */
    int deletePwProductByIds(Long[] ids);
}