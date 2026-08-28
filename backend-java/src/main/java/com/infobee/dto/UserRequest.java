package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserRequest(
    @NotBlank(message = "username is required") @Size(min = 3, max = 100) String username,
    @NotBlank(message = "password is required") @Size(min = 8, max = 100) @Schema(hidden = true) String password,
    @NotBlank(message = "fullName is required") @Size(max = 255) String fullName,
    @NotBlank(message = "role is required") @Size(max = 255) String role,
    Long departmentId
) {
}
