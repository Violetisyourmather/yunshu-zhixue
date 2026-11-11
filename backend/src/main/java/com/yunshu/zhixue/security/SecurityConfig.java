package com.yunshu.zhixue.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 双端安全配置（D7 / D20）：
 *
 * <ul>
 *   <li>{@code /api/admin/**}：管理端令牌域，除登录/刷新/登出外均需认证；接口另由
 *       {@link RequirePermission} 做权限点校验。</li>
 *   <li>{@code /api/portal/**}：学员端令牌域；首页与题库浏览公开（D4-2），
 *       其余需登录。</li>
 *   <li>其余路径（actuator / swagger / 静态资源）放行；{@code /files/**} 由 Nginx 直出。</li>
 * </ul>
 *
 * <p>同域部署，不配置 CORS（D20）。会话无状态，不启用表单登录与默认登出。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtTokenProvider tokenProvider, TokenBlacklist tokenBlacklist, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenProvider, tokenBlacklist);
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 登出放行以便重复调用幂等；服务端只拉黑请求自带的令牌
                        .requestMatchers("/api/admin/v1/auth/login", "/api/admin/v1/auth/refresh",
                                "/api/admin/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, ErrorCode.FORBIDDEN)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain portalFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/portal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 认证相关接口公开（refresh 由 X-Refresh-Token 自行校验）
                        .requestMatchers("/api/portal/v1/auth/**").permitAll()
                        // 首页公开
                        .requestMatchers("/api/portal/v1/home/**").permitAll()
                        // 平台基本信息（名称/描述/Logo/默认头像）需在未登录时渲染品牌
                        .requestMatchers(HttpMethod.GET, "/api/portal/v1/settings").permitAll()
                        // 题库中心浏览公开；解析与答案按登录态裁剪（D4-2）
                        .requestMatchers(HttpMethod.GET, "/api/portal/v1/bank/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, ErrorCode.FORBIDDEN)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            objectMapper.writeValue(response.getWriter(),
                    ApiResponse.fail(errorCode.code(), errorCode.message()));
        } catch (IOException ex) {
            log.warn("写出鉴权错误响应失败：{}", ex.getMessage());
        }
    }
}
