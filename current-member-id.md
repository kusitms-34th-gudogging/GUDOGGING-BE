# `@CurrentMemberId` 구현 가이드

> 컨트롤러 메서드 파라미터에 `@CurrentMemberId Long memberId`만 붙이면 **JWT 액세스 토큰에서 꺼낸 로그인 사용자 ID**가 자동으로 주입되는 기능의 구현 설명서입니다.
> 다른 프로젝트로 그대로 옮겨 쓸 수 있도록 전체 코드, 동작 흐름, 이식 체크리스트, 주의사항을 정리했습니다.

- 기준 환경: Java 21, Spring Boot 4.0.5 (Spring Security / Spring MVC), jjwt 0.13.0, springdoc-openapi 3.0.2, Lombok
- 원본 위치: `com.project.bangjjack.global.annotation`, `com.project.bangjjack.global.jwt`, `com.project.bangjjack.global.config`

---

## 1. 한눈에 보기

```java
@PostMapping
public CommonResponse<CreateRoommatePostResponse> createPost(
        @CurrentMemberId Long memberId,                 // ← 토큰의 sub 값이 들어옴
        @RequestBody @Valid CreateRoommatePostRequest request) {
    Long postId = roommatePostUseCase.createPost(memberId, request);
    ...
}
```

컨트롤러는 `SecurityContextHolder`, `Authentication`, `Principal` 같은 시큐리티 API를 전혀 몰라도 됩니다. "지금 요청한 사람의 ID"만 받습니다.

### 구성 요소

| 구성 요소 | 역할 |
|---|---|
| `JwtProvider` | 로그인 성공 시 액세스 토큰 발급 (`sub` = memberId) |
| `JwtAuthenticator` | 토큰 서명 검증 + Claims 파싱 |
| `JwtAuthenticationFilter` | 요청마다 `Authorization: Bearer ...` 헤더를 읽어 `SecurityContext`에 `MemberPrincipal`을 저장 |
| `MemberPrincipal` | 인증된 사용자 정보(memberId, memberName, role)를 담는 Principal 객체 |
| `@CurrentMemberId` | 컨트롤러 파라미터에 붙이는 마커 어노테이션 |
| `CurrentMemberIdArgumentResolver` | `SecurityContext` → `MemberPrincipal` → `memberId`를 꺼내 파라미터에 주입 |
| `WebMvcConfig` | 위 리졸버를 Spring MVC에 등록 |
| `SecurityConfig` | 필터 체인에 JWT 필터 등록, STATELESS 세션, 인증 실패 EntryPoint 지정 |
| `UnAuthorizedException` | 리졸버에서 인증 정보가 없을 때 던지는 예외 (401) |

---

## 2. 전체 동작 흐름

```
[Client]
   │  Authorization: Bearer eyJhbGciOi...
   ▼
┌──────────────────────────────────────────────────────────────┐
│ Servlet Filter Chain (Spring Security)                       │
│                                                              │
│  JwtAuthenticationFilter (UsernamePasswordAuthFilter 앞)      │
│   1) 헤더에서 토큰 추출 (없으면 그냥 통과)                         │
│   2) JwtAuthenticator.parseToken() → 서명/만료 검증              │
│   3) sub → memberId, name, role 꺼내기                         │
│   4) MemberPrincipal 생성                                      │
│   5) UsernamePasswordAuthenticationToken(principal, null,     │
│        authorities) 를 SecurityContext 에 저장                  │
│   ※ 실패 시: request.setAttribute("exception", e)              │
│             SecurityContext 비우고 계속 진행                     │
│                                                              │
│  AuthorizationFilter                                         │
│   - permitAll URL 이 아니고 인증 안 됐으면                         │
│     → JwtAuthenticationEntryPoint 가 401 JSON 응답             │
└──────────────────────────────────────────────────────────────┘
   ▼
┌──────────────────────────────────────────────────────────────┐
│ DispatcherServlet → HandlerAdapter                           │
│                                                              │
│  CurrentMemberIdArgumentResolver                             │
│   supportsParameter: @CurrentMemberId 붙었고 타입이 Long 인가?    │
│   resolveArgument:                                           │
│     SecurityContextHolder.getContext().getAuthentication()   │
│       → principal instanceof MemberPrincipal                 │
│       → memberPrincipal.getMemberId() 반환                    │
│     인증 없음 + required=true  → UnAuthorizedException (401)   │
│     인증 없음 + required=false → null                          │
└──────────────────────────────────────────────────────────────┘
   ▼
Controller(@CurrentMemberId Long memberId)
```

핵심은 **두 단계 분리**입니다.

1. **필터**는 "토큰이 유효하면 SecurityContext에 사용자 정보를 넣어둔다"만 책임집니다.
2. **ArgumentResolver**는 "SecurityContext에 있는 사용자 정보를 컨트롤러 파라미터로 꺼내준다"만 책임집니다.

DB 조회는 하지 않습니다. 토큰의 `sub` 클레임을 그대로 믿는 완전한 stateless 방식입니다.

---

## 3. 코드 전문

> 아래 코드는 이 프로젝트의 실제 코드입니다. 이식할 때는 패키지명만 바꾸면 됩니다.

### 3.1 `@CurrentMemberId` 어노테이션

```java
package com.project.bangjjack.global.annotation;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface CurrentMemberId {

    boolean required() default true;
}
```

| 요소 | 이유 |
|---|---|
| `@Target(PARAMETER)` | 메서드 파라미터에만 붙일 수 있게 제한 |
| `@Retention(RUNTIME)` | 리졸버가 런타임에 리플렉션으로 어노테이션을 확인해야 하므로 필수 |
| `@Parameter(hidden = true)` | springdoc(Swagger)이 이 파라미터를 **쿼리 파라미터로 오인해서 문서에 노출하지 않도록** 숨김. 메타 어노테이션으로 붙여 두었기 때문에 컨트롤러마다 따로 `@Parameter(hidden = true)`를 붙일 필요가 없음 |
| `required()` | 기본 `true`. `false`로 두면 비로그인 요청에서 예외 대신 `null`을 주입 (로그인/비로그인 모두 허용하는 API용) |

### 3.2 `CurrentMemberIdArgumentResolver`

```java
package com.project.bangjjack.global.annotation;

import com.project.bangjjack.global.common.exception.UnAuthorizedException;
import com.project.bangjjack.global.jwt.principal.MemberPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentMemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasAnnotation = parameter.hasParameterAnnotation(CurrentMemberId.class);
        boolean isLongType = Long.class.isAssignableFrom(parameter.getParameterType());
        return hasAnnotation && isLongType;
    }

    @Override
    public Long resolveArgument(MethodParameter parameter,
                                ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest,
                                WebDataBinderFactory binderFactory) {
        CurrentMemberId annotation = parameter.getParameterAnnotation(CurrentMemberId.class);
        if (annotation == null) {
            return null;
        }

        boolean required = annotation.required();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            if (required) throw new UnAuthorizedException();
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof MemberPrincipal memberPrincipal) {
            return memberPrincipal.getMemberId();
        }

        if (required) throw new UnAuthorizedException();
        return null;
    }
}
```

**`supportsParameter`**
- 어노테이션이 붙어 있고 **타입이 `Long`(박싱 타입)** 일 때만 이 리졸버가 동작합니다.
- Spring MVC는 리졸버 목록을 순서대로 돌며 `supportsParameter`가 `true`인 첫 리졸버를 사용하고, 결과를 파라미터별로 캐싱합니다.

**`resolveArgument`** — 분기별 결과

| 상황 | `required = true` (기본) | `required = false` |
|---|---|---|
| `Authentication`이 `null` | `UnAuthorizedException` | `null` |
| `isAuthenticated() == false` | `UnAuthorizedException` | `null` |
| principal이 `MemberPrincipal` | `memberId` 반환 | `memberId` 반환 |
| principal이 그 외 타입 (예: 익명 사용자 `"anonymousUser"` 문자열) | `UnAuthorizedException` | `null` |

> **왜 마지막 `instanceof` 분기가 중요한가?**
> Spring Security는 인증 정보가 없는 요청에 `AnonymousAuthenticationToken`을 넣어 둡니다. 이 토큰은 `isAuthenticated()`가 **`true`** 이고 principal은 문자열 `"anonymousUser"` 입니다.
> 즉 `permitAll` 경로에 토큰 없이 들어온 요청은 첫 번째 `if`를 통과해버립니다. 실제로 비로그인 요청을 걸러내는 것은 **`instanceof MemberPrincipal` 체크**입니다. 이식할 때 이 분기를 지우면 안 됩니다.

### 3.3 `WebMvcConfig` — 리졸버 등록

```java
package com.project.bangjjack.global.config.web;

import com.project.bangjjack.global.annotation.CurrentMemberIdArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentMemberIdArgumentResolver currentMemberIdArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberIdArgumentResolver);
    }
}
```

- `@Component`만 붙여서는 리졸버가 동작하지 않습니다. 반드시 `WebMvcConfigurer.addArgumentResolvers`로 등록해야 합니다.
- `addArgumentResolvers`로 추가한 커스텀 리졸버는 Spring 기본 리졸버(어노테이션 기반) 뒤, 기본 fallback 리졸버(`@RequestParam` 생략 처리 등) 앞에 들어갑니다. `@CurrentMemberId`는 기본 리졸버가 처리하는 어노테이션이 아니므로 충돌하지 않습니다.

### 3.4 `MemberPrincipal`

```java
package com.project.bangjjack.global.jwt.principal;

import com.project.bangjjack.global.jwt.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class MemberPrincipal implements UserDetails {

    private final Long memberId;
    private final String memberName;
    private final Role role;

    public static MemberPrincipal of(Long memberId, String memberName, Role role) {
        return new MemberPrincipal(memberId, memberName, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(memberId);
    }
}
```

- `UserDetails`를 구현해 두었기 때문에 `hasRole("MEMBER")` 같은 권한 체크에도 그대로 쓰입니다 (`ROLE_` 접두사 자동 부여).
- 정적 팩토리 `of()`만 열어두고 생성자는 `private`.

```java
package com.project.bangjjack.global.jwt;

public enum Role {
    MEMBER
}
```

### 3.5 `JwtProvider` — 토큰 발급

```java
package com.project.bangjjack.global.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey accessKey;
    private final long expirationSeconds;

    public JwtProvider(
            @Value("${jwt.access.secret}") String accessSecret,
            @Value("${jwt.access.expiration-seconds}") long expirationSeconds
    ) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        this.expirationSeconds = expirationSeconds;
    }

    public String createMemberAccessToken(Long memberId, String memberName) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("name", memberName)
                .claim("role", Role.MEMBER.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationSeconds * 1000L))
                .signWith(accessKey)
                .compact();
    }
}
```

토큰 페이로드 구조:

```json
{
  "sub": "42",          // memberId (문자열) ← @CurrentMemberId 의 원천
  "name": "홍길동",
  "role": "MEMBER",
  "iat": 1731600000,
  "exp": 1731859200
}
```

이 프로젝트에서는 `AuthUseCase`(OAuth 로그인 후 인가 코드 → 토큰 교환)와 `DevUseCase`(개발용 토큰 발급)에서 `createMemberAccessToken(user.getId(), user.getUsername())`을 호출합니다.

### 3.6 `JwtAuthenticator` — 토큰 검증

```java
package com.project.bangjjack.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtAuthenticator {

    private final SecretKey accessKey;

    public JwtAuthenticator(@Value("${jwt.access.secret}") String accessSecret) {
        this.accessKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

- `parseSignedClaims`는 서명 불일치, 만료, 형식 오류 시 각각 `SignatureException`, `ExpiredJwtException`, `MalformedJwtException` 등을 던집니다. 이 예외들은 필터에서 잡아 EntryPoint로 넘깁니다.
- 발급(`JwtProvider`)과 검증(`JwtAuthenticator`)을 클래스 단위로 분리해 두었습니다.

### 3.7 `JwtAuthenticationFilter`

```java
package com.project.bangjjack.global.jwt;

import com.project.bangjjack.global.jwt.principal.MemberPrincipal;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticator jwtAuthenticator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtAuthenticator.parseToken(token);
            Long memberId = Long.parseLong(claims.getSubject());
            String memberName = claims.get("name", String.class);
            Role role = Role.valueOf(claims.get("role", String.class));

            MemberPrincipal principal = MemberPrincipal.of(memberId, memberName, role);
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            request.setAttribute("exception", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

포인트:

1. **`@Component`가 아닙니다.** `SecurityConfig`에서 `new`로 생성해 시큐리티 필터 체인에만 넣습니다. `@Component`로 만들면 Spring Boot가 일반 서블릿 필터로도 자동 등록해서 **필터가 두 번 실행**될 수 있습니다.
2. **토큰이 없으면 그냥 통과**시킵니다. 인증이 필요한지 판단은 뒤의 `AuthorizationFilter`(`authorizeHttpRequests`)가 합니다. 덕분에 `permitAll` 경로는 토큰 없이 접근 가능합니다.
3. **토큰이 잘못돼도 여기서 응답을 끊지 않습니다.** 예외를 `request` 속성 `"exception"`에 담아두고 컨텍스트만 비운 뒤 진행합니다. 인증이 필요한 경로라면 `AuthorizationFilter`가 막고 `JwtAuthenticationEntryPoint`가 이 속성을 보고 에러 코드를 결정합니다.
4. `UsernamePasswordAuthenticationToken`의 **3-인자 생성자**를 사용해야 `authenticated = true`로 생성됩니다 (2-인자 생성자는 미인증 상태).
5. `sub`가 숫자가 아니거나 `role` 클레임이 없으면 `NumberFormatException` / `NullPointerException`이 나지만, `catch (Exception e)`로 모두 인증 실패 처리됩니다.

### 3.8 `SecurityConfig` (관련 부분)

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticator jwtAuthenticator;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final PermitUrlConfig permitUrlConfig;
    // (OAuth2 관련 빈은 생략)

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(permitUrlConfig.publicUrls()).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtAuthenticator),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- `STATELESS`: 세션을 만들지 않으므로 매 요청마다 토큰으로 인증합니다.
- `anyRequest().authenticated()`: 화이트리스트 외 모든 경로는 인증 필수 → 이 경로들에서는 리졸버에 도달한 시점에 이미 `MemberPrincipal`이 존재합니다.

### 3.9 `JwtAuthenticationEntryPoint` — 필터 단계 인증 실패 응답

```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object exceptionAttr = request.getAttribute("exception");
        Exception exception = (exceptionAttr instanceof Exception e) ? e : null;

        String code;
        String message;

        if (exception instanceof ExpiredJwtException) {
            code = "TOKEN_EXPIRED";
            message = "액세스 토큰이 만료되었습니다.";
        } else if (exception instanceof MalformedJwtException) {
            code = "INVALID_TOKEN";
            message = "유효하지 않은 토큰 형식입니다.";
        } else if (exception instanceof SignatureException) {
            code = "INVALID_SIGNATURE";
            message = "토큰 서명이 유효하지 않습니다.";
        } else {
            code = "UNAUTHORIZED";
            message = "인증이 필요합니다.";
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of("code", code, "message", message))
        );
    }
}
```

### 3.10 `UnAuthorizedException` — 리졸버 단계 인증 실패

```java
public class UnAuthorizedException extends ApplicationException {
    public UnAuthorizedException() {
        super(ErrorCode.UNAUTHORIZED);   // 40005, HttpStatus.UNAUTHORIZED, "인증되지 않았습니다."
    }
}
```

리졸버는 `DispatcherServlet` 안에서 실행되므로 이 예외는 `@RestControllerAdvice`(`GlobalExceptionHandler`)의 `@ExceptionHandler(ApplicationException.class)`가 처리해 `CommonResponse` 형식의 401 응답으로 변환됩니다.

### 3.11 설정값 (`application-*.yaml`)

```yaml
jwt:
  access:
    secret: ${JWT_ACCESS}          # Base64 인코딩된 HMAC 키 (디코딩 후 256bit 이상)
    expiration-seconds: 259200     # 3일
```

비밀키 생성 예시:

```bash
openssl rand -base64 64
```

### 3.12 의존성 (`build.gradle`)

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'   // Boot 3.x 이하는 spring-boot-starter-web
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2'   // @Parameter(hidden) 용

implementation 'io.jsonwebtoken:jjwt-api:0.13.0'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.13.0'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.13.0'

compileOnly 'org.projectlombok:lombok'
annotationProcessor 'org.projectlombok:lombok'
```

---

## 4. 에러 응답이 두 경로로 나뉜다

인증 실패는 **어디서 막히느냐**에 따라 응답 형식이 다릅니다. 프론트와 맞출 때 알고 있어야 합니다.

| 막히는 위치 | 언제 | 응답 형식 |
|---|---|---|
| 필터 단계 (`JwtAuthenticationEntryPoint`) | 인증 필수 경로에 토큰 없음/만료/위조 | `{"code": "TOKEN_EXPIRED", "message": "..."}` (문자열 code) |
| 리졸버 단계 (`UnAuthorizedException`) | `permitAll` 경로인데 `@CurrentMemberId`(required=true)를 쓴 경우 | `CommonResponse.error(ErrorCode.UNAUTHORIZED)` (숫자 code `40005`) |

일반적인 인증 필수 API에서는 항상 필터 단계에서 먼저 막히므로, 리졸버의 예외는 사실상 **"permitAll 경로에서 실수로 required=true를 쓴 경우"의 안전망**입니다. 새 프로젝트에서는 두 응답 형식을 하나로 통일하는 것을 고려해 보세요.

---

## 5. 사용법

### 5.1 기본 (로그인 필수)

```java
@GetMapping("/me")
public CommonResponse<List<MyRoommatePostResponse>> getMyPosts(@CurrentMemberId Long memberId) {
    return CommonResponse.success(..., roommatePostUseCase.getMyPosts(memberId));
}
```

### 5.2 선택 로그인 (`required = false`)

비로그인도 허용하되, 로그인한 경우 추가 정보(예: 내가 북마크했는지)를 보여주고 싶을 때. 해당 경로는 `PermitUrlConfig`에 `permitAll`로 등록되어 있어야 합니다.

```java
@GetMapping("/{postId}")
public CommonResponse<PostDetailResponse> getPost(
        @CurrentMemberId(required = false) Long memberId,   // 비로그인이면 null
        @PathVariable Long postId) {
    ...
}
```

> 현재 이 프로젝트에서는 `required = false`를 사용하는 엔드포인트는 없습니다.

### 5.3 Swagger

`@Parameter(hidden = true)`가 메타 어노테이션으로 붙어 있어 Swagger UI에 `memberId` 입력란이 나타나지 않습니다. Swagger에서 테스트할 때는 우측 상단 **Authorize** 버튼에 Bearer 토큰을 넣으면 됩니다 (`SwaggerConfig`에 bearer `SecurityScheme` 등록 필요).

---

## 6. 다른 프로젝트로 이식하기 — 체크리스트

1. [ ] 의존성 추가 (3.12)
2. [ ] `jwt.access.secret`, `jwt.access.expiration-seconds` 설정 및 환경변수 등록 (3.11)
3. [ ] 아래 파일 복사 후 **패키지명 변경**
   - `annotation/CurrentMemberId.java`
   - `annotation/CurrentMemberIdArgumentResolver.java`
   - `config/web/WebMvcConfig.java` (이미 `WebMvcConfigurer`가 있으면 `addArgumentResolvers`만 합치기)
   - `jwt/JwtProvider.java`, `jwt/JwtAuthenticator.java`, `jwt/JwtAuthenticationFilter.java`, `jwt/Role.java`
   - `jwt/principal/MemberPrincipal.java`
   - `config/security/JwtAuthenticationEntryPoint.java`
   - `common/exception/UnAuthorizedException.java` → 새 프로젝트의 예외 체계에 맞게 교체 (기존 `ApplicationException`/`ErrorCode`가 없다면 `ResponseStatusException(HttpStatus.UNAUTHORIZED)` 등으로 대체 가능)
4. [ ] `SecurityConfig`에 다음 반영
   - `SessionCreationPolicy.STATELESS`
   - `.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))`
   - `.addFilterBefore(new JwtAuthenticationFilter(jwtAuthenticator), UsernamePasswordAuthenticationFilter.class)`
   - `permitAll` 화이트리스트 정의
5. [ ] 로그인 로직에서 `jwtProvider.createMemberAccessToken(userId, username)` 호출해 토큰 발급
6. [ ] Swagger를 쓰지 않는다면 `@Parameter(hidden = true)`와 springdoc 의존성 제거
7. [ ] 동작 확인
   - 토큰 없이 인증 필수 API → 401 (`UNAUTHORIZED`)
   - 만료 토큰 → 401 (`TOKEN_EXPIRED`)
   - 정상 토큰 → 컨트롤러에서 `memberId` 값 확인

### 사용자 ID 타입이 `Long`이 아니라면

예: UUID를 쓰는 프로젝트

- `MemberPrincipal.memberId` 타입 → `UUID`
- 필터의 `Long.parseLong(claims.getSubject())` → `UUID.fromString(...)`
- 리졸버의 `supportsParameter` → `UUID.class.isAssignableFrom(...)`, 반환 타입 → `UUID`
- 컨트롤러 → `@CurrentMemberId UUID memberId`

---

## 7. 주의사항 / 함정

### 7.1 반드시 `Long`(박싱 타입)으로 선언할 것 — `long` 금지

```java
@CurrentMemberId long memberId   // ❌ 위험
@CurrentMemberId Long memberId   // ✅
```

`Long.class.isAssignableFrom(long.class)`는 `false`이므로 primitive `long`으로 선언하면 **이 리졸버가 선택되지 않습니다.** 그러면 Spring의 기본 fallback 리졸버가 이 파라미터를 **이름이 `memberId`인 쿼리 파라미터**로 취급합니다. 결과적으로:

- 쿼리 파라미터가 없으면 400 에러
- **클라이언트가 `?memberId=1`을 붙이면 그 값이 그대로 들어감 → 다른 사용자로 위장 가능**

컴파일 에러가 나지 않으므로 코드 리뷰에서 반드시 확인하세요. (보완하고 싶다면 `supportsParameter`에서 어노테이션만 보고 `true`를 반환한 뒤, `resolveArgument` 쪽에서 타입이 맞지 않으면 `IllegalStateException`을 던지게 바꾸는 방법이 있습니다.)

### 7.2 DB를 조회하지 않는다

`memberId`는 토큰의 `sub` 값 그대로입니다. 탈퇴/정지된 사용자의 토큰도 만료 전까지는 유효하게 통과합니다. 사용자 존재 여부가 중요한 로직은 UseCase/Service에서 `findById` 후 검증해야 합니다.

### 7.3 `SecurityContextHolder`는 ThreadLocal 기반

- `@Async`, `CompletableFuture`, 별도 스레드풀에서는 `SecurityContext`가 전파되지 않습니다. 컨트롤러에서 받은 `memberId`를 **값으로 넘기세요.** (이것이 이 패턴의 장점이기도 합니다 — 하위 계층이 시큐리티에 의존하지 않음)
- WebSocket/STOMP 메시지 핸들러는 HTTP 필터 체인을 타지 않으므로 이 리졸버가 동작하지 않습니다. 이 프로젝트는 WebSocket 인증을 `WsAuthHandshakeInterceptor`로 별도 처리합니다.

### 7.4 필터 이중 등록 방지

`JwtAuthenticationFilter`에 `@Component`를 붙이지 마세요. 붙여야 한다면 `FilterRegistrationBean`으로 서블릿 자동 등록을 꺼야 합니다 (`registration.setEnabled(false)`).

### 7.5 `isAuthenticated()`만으로는 비로그인을 걸러낼 수 없다

3.2의 설명처럼 익명 토큰은 `isAuthenticated() == true`입니다. 리졸버의 `instanceof MemberPrincipal` 분기를 유지하세요.

---

## 8. 테스트 작성 예시

> 이 프로젝트에는 아직 `@CurrentMemberId` 전용 테스트가 없습니다. 아래는 이식 후 추가하면 좋은 테스트 예시입니다.

### 8.1 리졸버 단위 테스트

```java
class CurrentMemberIdArgumentResolverTest {

    private final CurrentMemberIdArgumentResolver resolver = new CurrentMemberIdArgumentResolver();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 테스트용 더미 메서드
    @SuppressWarnings("unused")
    void handler(@CurrentMemberId Long required,
                 @CurrentMemberId(required = false) Long optional,
                 @CurrentMemberId long primitive) {}

    private MethodParameter param(int index) throws NoSuchMethodException {
        Method m = getClass().getDeclaredMethod("handler", Long.class, Long.class, long.class);
        return new MethodParameter(m, index);
    }

    @Test
    void 인증된_사용자의_memberId를_반환한다() throws Exception {
        MemberPrincipal principal = MemberPrincipal.of(42L, "tester", Role.MEMBER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertThat(resolver.resolveArgument(param(0), null, null, null)).isEqualTo(42L);
    }

    @Test
    void 비로그인이고_required이면_예외() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(param(0), null, null, null))
                .isInstanceOf(UnAuthorizedException.class);
    }

    @Test
    void 익명_사용자이고_required_false이면_null() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(resolver.resolveArgument(param(1), null, null, null)).isNull();
    }

    @Test
    void primitive_long은_지원하지_않는다() throws Exception {
        assertThat(resolver.supportsParameter(param(2))).isFalse();
    }
}
```

### 8.2 컨트롤러 슬라이스 테스트에서 인증 사용자 주입

```java
SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
                MemberPrincipal.of(1L, "tester", Role.MEMBER), null,
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))));

mockMvc.perform(get("/api/v1/posts/me"))
        .andExpect(status().isOk());
```

또는 `spring-security-test`의 `.with(authentication(...))` / `@WithSecurityContext` 커스텀 어노테이션을 만들어 쓰면 반복 코드를 줄일 수 있습니다.
