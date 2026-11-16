package com.subscribers.gudogging.global.security.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 Access Token의 로그인 회원 ID(UUID)를 주입한다. 사용: {@code @CurrentUserId UUID userId}
 * <p>
 * Swagger 문서에는 노출되지 않는다(@Parameter(hidden = true) 메타 어노테이션).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface CurrentUserId {
}
