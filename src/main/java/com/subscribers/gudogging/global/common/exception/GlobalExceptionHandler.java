package com.subscribers.gudogging.global.common.exception;

import com.subscribers.gudogging.global.common.response.CommonResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<CommonResponse<Void>> handleException(ApplicationException e) {
        ErrorCodeInterface errorCode = e.getErrorCode();
        String errorMessage = e.getMessage() != null ? e.getMessage() : errorCode.getMessage();
        CommonResponse<Void> body = CommonResponse.error(errorCode, errorMessage);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<List<ErrorResponse>>> handleValidation(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;

        List<ErrorResponse> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(fe -> ErrorResponse.of(
                fe.getField(),
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "",
                fe.getRejectedValue()
            ))
            .collect(Collectors.toList());

        CommonResponse<List<ErrorResponse>> body = CommonResponse.error(errorCode, errors);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<CommonResponse<Void>> handleDuplicateKey() {
        ErrorCode errorCode = ErrorCode.DUPLICATE_RESOURCE;
        CommonResponse<Void> body = CommonResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleDataIntegrityViolation() {
        ErrorCode errorCode = ErrorCode.DATA_INTEGRITY_ERROR;
        CommonResponse<Void> body = CommonResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleIllegalArgument() {
        ErrorCode errorCode = ErrorCode.INVALID_ARGUMENT;
        CommonResponse<Void> body = CommonResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoResourceFound() {
        ErrorCode errorCode = ErrorCode.RESOURCE_NOT_FOUND;
        CommonResponse<Void> body = CommonResponse.error(errorCode);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        CommonResponse<Void> body = CommonResponse.error(errorCode);

        return ResponseEntity
            .status(e.getStatusCode().value())
            .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof ApplicationException appEx) {
            ErrorCodeInterface errorCode = appEx.getErrorCode();
            String msg = ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage();
            CommonResponse<Void> body = CommonResponse.error(errorCode, msg);

            return ResponseEntity
                .status(errorCode.getStatus())
                .body(body);
        }

        ErrorCode errorCode = ErrorCode.JSON_PARSE_ERROR;
        String msg = ex.getMessage() != null ? ex.getMessage() : errorCode.getMessage();
        CommonResponse<Void> body = CommonResponse.error(errorCode, msg);

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(body);
    }
}
