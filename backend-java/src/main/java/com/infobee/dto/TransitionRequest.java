package com.infobee.dto;

import jakarta.validation.constraints.Size;

public record TransitionRequest(@Size(max = 2000, message = "note must be at most 2000 characters") String note) {}
