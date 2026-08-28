package com.infobee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodeGroupRequest(
    @NotBlank @Size(max = 50) String groupCode,
    @NotBlank @Size(max = 200) String groupName,
    @Size(max = 2000) String description
) {}
