package com.pinwu.app.modules.order.mapper;

import com.pinwu.app.modules.order.domain.entity.PwOrderItem;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface PwOrderItemMapper {
    int insertPwOrderItem(PwOrderItem item);
    List<PwOrderItem> selectItemsByOrderNo(String orderNo);
}