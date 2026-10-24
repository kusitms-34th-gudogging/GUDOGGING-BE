package com.subscribers.gudogging.domain.example.application.usecase;

import com.subscribers.gudogging.domain.example.application.dto.request.CreatePostRequest;
import com.subscribers.gudogging.domain.example.application.dto.response.PostResponse;
import com.subscribers.gudogging.domain.example.domain.entity.Post;
import com.subscribers.gudogging.domain.example.domain.service.PostCreateService;
import com.subscribers.gudogging.domain.example.domain.service.PostGetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostUseCase {

    private final PostCreateService postCreateService;
    private final PostGetService postGetService;

    @Transactional
    public PostResponse createPost(CreatePostRequest request) {
        Post post = Post.create(request.title(), request.content(), request.authorName());
        Post saved = postCreateService.createPost(post);
        return PostResponse.from(saved);
    }

    public List<PostResponse> getAllPosts() {
        return postGetService.getAllPosts().stream()
            .map(PostResponse::from)
            .toList();
    }

    public PostResponse getPost(Long id) {
        Post post = postGetService.getPost(id);
        return PostResponse.from(post);
    }
}
