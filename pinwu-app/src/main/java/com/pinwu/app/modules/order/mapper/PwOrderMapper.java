package com.pinwu.app.modules.order.mapper;

import com.pinwu.app.modules.order.domain.entity.PwOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PwOrderMapper {
    /**
     * 新增订单 (必须返回 ID)
     */
    int insertPwOrder(PwOrder pwOrder);

    /**
     * 根据订单号查询
     */
    PwOrder selectPwOrderByOrderNo(String orderNo);
    
    /**
     * 更新订单状态
     */
    int updatePwOrder(PwOrder pwOrder);

    // 新增查询方法
    List<PwOrder> selectOrderList(@Param("userId") Long userId,
                                  @Param("role") String role,
                                  @Param("status") Integer status);
}