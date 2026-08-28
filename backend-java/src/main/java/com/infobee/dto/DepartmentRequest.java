package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank(message = "department name is required") @Size(max = 255) String name
) {
}
