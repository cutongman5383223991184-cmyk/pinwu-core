package com.pinwu.app.modules.user.mapper;

import com.pinwu.app.modules.user.domain.PwUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PwUserMapper {
    PwUser selectPwUserByMobile(@Param("mobile") String mobile);
    PwUser selectPwUserById(Long id);
    int insertPwUser(PwUser user);
    int updatePwUser(PwUser user);
}