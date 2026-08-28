package com.infobee.dto;

import com.infobee.model.RequestComment;
import java.time.Instant;

public record CommentResponse(Long id, Long authorId, String authorUsername, String body, Instant createdAt) {
    public static CommentResponse from(RequestComment comment) {
        return new CommentResponse(comment.getId(), comment.getAuthor().getId(), comment.getAuthor().getUsername(),
            comment.getBody(), comment.getCreatedAt());
    }
}
