package com.iflytek.astron.console.hub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Casdoor配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "casdoor")
public class CasdoorConfig {

    /**
     * Casdoor服务端点
     */
    private String endpoint;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端密钥
     */
    private String clientSecret;

    /**
     * 登录使用的组织（admin 用户属于 built-in 组织）
     */
    private String loginOrganization;

    /**
     * 登录使用的应用
     */
    private String loginApplication;

    /**
     * 新用户所属组织（注册用户需要配置的组织）
     */
    private String userOrganization;

    /**
     * 管理员用户名（用于调用管理接口）
     */
    private String adminUsername;

    /**
     * 管理员密码（用于调用管理接口）
     */
    private String adminPassword;
}
