package com.subscribers.gudogging.global.security.jwt;

import com.subscribers.gudogging.domain.auth.application.exception.AuthErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SecurityConfig에서 new로 등록한다. @Component로 등록하면 서블릿 필터로도 자동 등록되어 두 번 실행된다. 인증 실패 시 예외를 던지지 않고 에러 코드를 request attribute에
 * 남긴다. 보호 API면 JwtAuthenticationEntryPoint가 응답한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE_ATTRIBUTE = "jwtErrorCode";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                UUID userId = jwtProvider.parseAccessToken(token);
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ERROR_CODE_ATTRIBUTE, AuthErrorCode.EXPIRED_ACCESS_TOKEN);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ERROR_CODE_ATTRIBUTE, AuthErrorCode.INVALID_ACCESS_TOKEN);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
