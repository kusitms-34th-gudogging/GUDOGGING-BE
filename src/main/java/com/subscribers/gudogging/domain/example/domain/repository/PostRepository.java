package com.subscribers.gudogging.domain.example.domain.repository;

import com.subscribers.gudogging.domain.example.domain.entity.Post;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findAllByDeletedFalse();

    Optional<Post> findByIdAndDeletedFalse(UUID id);
}
