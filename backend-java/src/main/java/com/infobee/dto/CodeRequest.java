package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CodeRequest(
    @NotNull Long groupId,
    @NotBlank @Size(max = 50) String codeValue,
    @NotBlank @Size(max = 200) String codeLabel,
    Integer sortOrder
) {}
