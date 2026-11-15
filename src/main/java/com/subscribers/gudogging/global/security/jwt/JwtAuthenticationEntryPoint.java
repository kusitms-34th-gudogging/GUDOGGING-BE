package com.subscribers.gudogging.global.security.jwt;

import com.subscribers.gudogging.global.common.exception.ErrorCode;
import com.subscribers.gudogging.global.common.exception.ErrorCodeInterface;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorCodeInterface errorCode = resolveErrorCode(request);

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(), CommonResponse.error(errorCode));
    }

    private ErrorCodeInterface resolveErrorCode(HttpServletRequest request) {
        Object errorCode = request.getAttribute(JwtAuthenticationFilter.ERROR_CODE_ATTRIBUTE);
        if (errorCode instanceof ErrorCodeInterface code) {
            return code;
        }
        return ErrorCode.UNAUTHORIZED;
    }
}
