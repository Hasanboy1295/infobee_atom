package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(@NotBlank(message = "comment body is required") @Size(max = 5000) String body) {}
