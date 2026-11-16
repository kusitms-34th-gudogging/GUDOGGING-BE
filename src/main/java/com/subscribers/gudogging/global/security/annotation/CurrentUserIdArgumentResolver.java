package com.subscribers.gudogging.global.security.annotation;

import com.subscribers.gudogging.global.common.exception.UnAuthorizedException;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * JwtAuthenticationFilter가 SecurityContext에 넣어 둔 principal(userId, UUID)을 @CurrentUserId 파라미터로 꺼낸다.
 * <p>
 * 어노테이션만 보고 지원 여부를 정한다. 타입까지 보면 잘못된 타입으로 선언했을 때 이 리졸버가 선택되지 않고 기본 리졸버가 같은 이름의 쿼리 파라미터로 바인딩해, 클라이언트가 userId를 임의로 넣을 수 있게
 * 된다. 그래서 타입이 틀리면 바인딩하지 않고 예외를 던진다.
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class);
    }

    @Override
    public UUID resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!UUID.class.equals(parameter.getParameterType())) {
            throw new IllegalStateException("@CurrentUserId는 UUID 타입 파라미터에만 사용할 수 있습니다: " + parameter);
        }

        // 토큰 없이 permitAll 경로로 들어오면 principal은 익명 사용자 문자열("anonymousUser")이므로 타입으로 걸러낸다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        throw new UnAuthorizedException();
    }
}
