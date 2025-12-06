//package com.pinwu.app.modules.ai.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//
//@Configuration
//public class AppSecurityConfig extends WebSecurityConfigurerAdapter {
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//            // 关闭 CSRF
//            .csrf().disable()
//            // 允许所有请求，或者只放行特定接口
//            .authorizeRequests()
//            .antMatchers("/app/ai/**").permitAll() // 放行 AI 相关接口
//                .antMatchers("/app/auth/**").permitAll()
//            .anyRequest().authenticated(); // 其他接口需要登录
//    }
//}