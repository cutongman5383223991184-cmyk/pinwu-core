package com.pinwu.app.modules.auth.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.pinwu.app.modules.auth.domain.model.AppLoginUser; // ★ 引用新的 User
import com.pinwu.app.modules.auth.domain.vo.AppLoginBody;
import com.pinwu.app.modules.user.domain.PwUser;
import com.pinwu.app.modules.user.mapper.PwUserMapper;
import com.pinwu.common.core.redis.RedisCache;
import com.pinwu.common.exception.ServiceException;
import com.pinwu.common.utils.DateUtils;
import com.pinwu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AppAuthService {

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private PwUserMapper pwUserMapper;

    @Autowired
    private AppTokenService appTokenService; // ★ 引用新的 Service

    private static final String SMS_CODE_PREFIX = "app:sms:code:";

    // 发送验证码 (Mock)
    public String sendSmsCode(String mobile) {
        if (StrUtil.isBlank(mobile) || mobile.length() != 11) {
            throw new ServiceException("手机号格式不正确");
        }
        String code = RandomUtil.randomNumbers(6);
        redisCache.setCacheObject(SMS_CODE_PREFIX + mobile, code, 5, TimeUnit.MINUTES);
        System.out.println("【APP模拟短信】" + mobile + " : " + code);
        return code;
    }

    // 登录核心
    public String login(AppLoginBody loginBody) {
        String mobile = loginBody.getMobile();
        String type = loginBody.getLoginType();

        PwUser user = pwUserMapper.selectPwUserByMobile(mobile);

        if ("SMS".equalsIgnoreCase(type)) {
            validateSmsCode(mobile, loginBody.getCode());
            if (user == null) {
                user = registerUser(mobile);
            }
        } else if ("PASSWORD".equalsIgnoreCase(type)) {
            if (user == null) {
                throw new ServiceException("账号不存在");
            }
            if (!BCrypt.checkpw(loginBody.getPassword(), user.getPassword())) {
                throw new ServiceException("密码错误");
            }
        } else {
            throw new ServiceException("不支持的登录方式");
        }

        // 更新登录信息
        user.setLoginDate(DateUtils.getNowDate());
        user.setLoginIp("127.0.0.1");
        pwUserMapper.updatePwUser(user);

        // ★★★ 核心修复：使用 AppLoginUser，不再报错 setUsername 找不到
        AppLoginUser loginUser = new AppLoginUser(user);

        // 生成 Token
        return appTokenService.createToken(loginUser);
    }

    private void validateSmsCode(String mobile, String inputCode) {
        String key = SMS_CODE_PREFIX + mobile;
        String cacheCode = redisCache.getCacheObject(key);
        if (StringUtils.isEmpty(cacheCode) || !cacheCode.equals(inputCode)) {
            throw new ServiceException("验证码无效或已过期");
        }
        redisCache.deleteObject(key);
    }

    private PwUser registerUser(String mobile) {
        PwUser newUser = new PwUser();
        newUser.setMobile(mobile);
        newUser.setUserName("pw_" + RandomUtil.randomString("abcdef0123456789", 8));
        newUser.setNickname("拼友" + RandomUtil.randomNumbers(4));
        newUser.setStatus("0");
        newUser.setCreateTime(DateUtils.getNowDate());
        pwUserMapper.insertPwUser(newUser);
        return newUser;
    }
}