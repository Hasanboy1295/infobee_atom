package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
    @NotBlank(message = "username is required") String username,
    @NotBlank(message = "password is required") @Schema(hidden = true) String password
) {
}
