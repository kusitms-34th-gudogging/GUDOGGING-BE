package com.subscribers.gudogging.domain.example.domain.service;

import com.subscribers.gudogging.domain.example.application.exception.PostNotFoundException;
import com.subscribers.gudogging.domain.example.domain.entity.Post;
import com.subscribers.gudogging.domain.example.domain.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostGetService {

    private final PostRepository postRepository;

    public List<Post> getAllPosts() {
        return postRepository.findAllByDeletedFalse();
    }

    public Post getPost(UUID id) {
        return postRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(PostNotFoundException::new);
    }
}
