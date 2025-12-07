package com.pinwu.app.modules.user.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pinwu.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

/**
 * C端用户对象 pw_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PwUser extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;

    /** ★ 新增：用户账号 (系统自动生成，如 pw_x8s7) */
    private String userName;

    /** 用户昵称 (如 拼友1234) */
    private String nickname;

    /** 手机号码 */
    private String mobile;

    /** 密码 (加密存储) */
    private String password;

    /** 头像地址 */
    private String avatar;

    /** 帐号状态（0正常 1停用） */
    private String status;

    /** 最后登录IP */
    private String loginIp;

    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date loginDate;

    /** 会员过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date vipExpireTime;
}