package com.pinwu.app.modules.auth.domain.vo;

import lombok.Data;

@Data
public class AppLoginBody {
    private String mobile;
    private String loginType; // SMS 或 PASSWORD
    private String code;
    private String password;
}