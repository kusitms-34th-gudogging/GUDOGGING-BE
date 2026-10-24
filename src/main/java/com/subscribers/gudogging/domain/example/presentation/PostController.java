package com.subscribers.gudogging.domain.example.presentation;

import com.subscribers.gudogging.domain.example.application.dto.request.CreatePostRequest;
import com.subscribers.gudogging.domain.example.application.dto.response.PostResponse;
import com.subscribers.gudogging.domain.example.application.usecase.PostUseCase;
import com.subscribers.gudogging.domain.example.presentation.response.PostResponseCode;
import com.subscribers.gudogging.global.common.response.CommonResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/example/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostUseCase postUseCase;

    @PostMapping
    public CommonResponse<PostResponse> createPost(@RequestBody @Valid CreatePostRequest request) {
        PostResponse response = postUseCase.createPost(request);
        return CommonResponse.success(PostResponseCode.POST_CREATED, response);
    }

    @GetMapping
    public CommonResponse<List<PostResponse>> getAllPosts() {
        List<PostResponse> response = postUseCase.getAllPosts();
        return CommonResponse.success(PostResponseCode.POSTS_FOUND, response);
    }

    @GetMapping("/{postId}")
    public CommonResponse<PostResponse> getPost(@PathVariable Long postId) {
        PostResponse response = postUseCase.getPost(postId);
        return CommonResponse.success(PostResponseCode.POST_FOUND, response);
    }
}
