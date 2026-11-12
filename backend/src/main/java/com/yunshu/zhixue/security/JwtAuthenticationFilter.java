package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 双端 JWT 认证过滤器：按请求路径前缀判定令牌域（{@code /api/admin/**} → admin，
 * {@code /api/portal/**} → portal），校验通过后写入 SecurityContext。
 *
 * <p>令牌缺失或无效时不在此处直接返回错误，交由 Spring Security 的
 * AuthenticationEntryPoint 统一输出 401 + 10001。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 权限码在 GrantedAuthority 中的前缀，与 {@link PermissionAspect} 约定一致。 */
    public static final String PERMISSION_PREFIX = "PERM_";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenBlacklist tokenBlacklist) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    /**
     * SSE 等异步请求完成时会再次经过过滤器链，需要重新建立 SecurityContext，
     * 否则异步分发会被 AuthorizationFilter 判定为未认证。
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Audience audience = resolveAudience(request);
        String token = resolveToken(request);

        if (audience != null && token != null) {
            try {
                JwtPayload payload = tokenProvider.parse(token, audience, TokenType.ACCESS);
                if (!tokenBlacklist.isRevoked(payload.jti())) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority(
                            audience == Audience.ADMIN ? "ROLE_ADMIN" : "ROLE_PORTAL"));
                    payload.permissions().forEach(
                            p -> authorities.add(new SimpleGrantedAuthority(PERMISSION_PREFIX + p)));

                    var authentication = new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(payload.userId(), audience, payload.permissions()),
                            null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (BizException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private Audience resolveAudience(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin/")) {
            return Audience.ADMIN;
        }
        if (uri.startsWith("/api/portal/")) {
            return Audience.PORTAL;
        }
        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }
}
