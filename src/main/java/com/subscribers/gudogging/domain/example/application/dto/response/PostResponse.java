package com.subscribers.gudogging.domain.example.application.dto.response;

import com.subscribers.gudogging.domain.example.domain.entity.Post;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
	UUID id,
	String title,
	String content,
	String authorName,
	LocalDateTime createdAt
) {
	public static PostResponse from(Post post) {
		return new PostResponse(
			post.getId(),
			post.getTitle(),
			post.getContent(),
			post.getAuthorName(),
			post.getCreatedAt()
		);
	}
}
