package com.campus.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码安全组件配置。
 *
 * <p>通过 {@code @Bean} 把 PasswordEncoder 放入 Spring 容器，
 * AuthService 只依赖接口，不需要自己 new BCryptPasswordEncoder。</p>
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt 每次加密都会生成随机盐，相同明文也不会得到相同哈希。
     *
     * <p>强度 10 在开发机上兼顾安全性和响应速度。数据库只保存该哈希，
     * 登录时通过 matches 校验，任何位置都不能记录或回传明文密码。</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
