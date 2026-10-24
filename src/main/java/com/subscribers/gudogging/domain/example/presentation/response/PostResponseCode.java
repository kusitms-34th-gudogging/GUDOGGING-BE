package com.subscribers.gudogging.domain.example.presentation.response;

import com.subscribers.gudogging.global.common.response.ResponseCodeInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostResponseCode implements ResponseCodeInterface {

    POST_CREATED(201, HttpStatus.CREATED, "게시글이 생성되었습니다."),
    POSTS_FOUND(200, HttpStatus.OK, "게시글 목록을 조회했습니다."),
    POST_FOUND(200, HttpStatus.OK, "게시글을 조회했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
