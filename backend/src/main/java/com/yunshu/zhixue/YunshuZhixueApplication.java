package com.yunshu.zhixue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 云枢智学后端启动类。
 *
 * <p>模块化单体：业务代码按领域放在 {@code com.yunshu.zhixue.modules.<领域>} 下，
 * 模块间单向依赖，只经 Service 接口与 DTO 交互（02 §3.2）。</p>
 *
 * <p>鉴权完全由双端 JWT 承担，不使用表单登录，故排除
 * {@link UserDetailsServiceAutoConfiguration}，避免启动日志输出无用的随机密码。</p>
 */
@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class YunshuZhixueApplication {

    public static void main(String[] args) {
        SpringApplication.run(YunshuZhixueApplication.class, args);
    }
}
