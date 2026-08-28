package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record SignupRequest(
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 100, message = "username must be 3-100 characters")
    String username,
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be 8-100 characters") @Schema(hidden = true)
    String password,
    @NotBlank(message = "fullName is required")
    @Size(max = 255, message = "fullName must be at most 255 characters")
    String fullName
) {
}
