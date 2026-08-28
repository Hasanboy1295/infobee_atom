package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
    @NotBlank(message = "role name is required") @Size(max = 255) String name
) {
}
