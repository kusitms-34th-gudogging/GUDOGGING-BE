package com.subscribers.gudogging.domain.example.domain.service;

import com.subscribers.gudogging.domain.example.domain.entity.Post;
import com.subscribers.gudogging.domain.example.domain.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PostCreateService {

    private final PostRepository postRepository;

    public Post createPost(Post post) {
        return postRepository.save(post);
    }
}
