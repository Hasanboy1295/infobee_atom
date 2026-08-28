package com.infobee.dto;

import java.util.List;

public record BatchResult(
    int succeeded,
    int failed,
    List<BatchResultItem> details
) {
    public record BatchResultItem(Long id, boolean success, String message) {}
}
