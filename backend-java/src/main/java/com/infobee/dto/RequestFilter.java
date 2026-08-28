package com.infobee.dto;

public record RequestFilter(
    String status,
    String priority,
    Long departmentId,
    String createdFrom,
    String createdTo,
    String search
) {
    public static RequestFilter empty() {
        return new RequestFilter(null, null, null, null, null, null);
    }
}
