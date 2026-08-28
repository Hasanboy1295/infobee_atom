package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MenuRequest(
    @NotBlank(message = "menu label is required") @Size(max = 255) String label,
    @NotBlank(message = "menu path is required") @Size(max = 255) String path
) {
}
