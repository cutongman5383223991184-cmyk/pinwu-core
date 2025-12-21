package com.pinwu.common.core.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pinwu.common.core.domain.entity.PwUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

/**
 * App 端登录用户身份权限对象
 * 专门用于 C 端用户，与后台 SysUser 彻底隔离
 */
@Data
@NoArgsConstructor
public class AppLoginUser implements UserDetails {
    private static final long serialVersionUID = 1L;

    /** 用户唯一标识 (使用我们生成的 openId 或 userId) */
    private String token;

    /** 登录时间 */
    private Long loginTime;

    /** 过期时间 */
    private Long expireTime;

    /** * ★ 核心：直接持有 PwUser，而不是 SysUser
     */
    private PwUser pwUser;

    public AppLoginUser(PwUser pwUser) {
        this.pwUser = pwUser;
    }

    // --- 以下是 Spring Security 必须实现的方法 ---

    @JsonIgnore
    @Override
    public String getPassword() {
        return pwUser.getPassword();
    }

    @Override
    public String getUsername() {
        return pwUser.getUserName(); // 使用我们生成的 pw_xxxx
    }

    /** * App端一般不需要复杂的 Role/Permission 权限校验，
     * 如果有，这里返回，暂时返回空即可 
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null; 
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { 
        return "0".equals(pwUser.getStatus()); // 0正常
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { 
        return "0".equals(pwUser.getStatus()); 
    }
}