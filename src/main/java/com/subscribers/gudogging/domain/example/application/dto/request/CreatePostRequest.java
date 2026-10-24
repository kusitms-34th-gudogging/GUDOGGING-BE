package com.subscribers.gudogging.domain.example.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
	@NotBlank @Size(max = 100) String title,
	@NotBlank @Size(max = 2000) String content,
	@NotBlank @Size(max = 50) String authorName
) {
}
