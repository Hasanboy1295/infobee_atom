package com.infobee.dto;

import java.util.List;

public record RequestPageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
