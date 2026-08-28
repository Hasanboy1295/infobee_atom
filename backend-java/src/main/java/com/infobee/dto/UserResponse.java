package com.infobee.dto;

import com.infobee.model.User;

public record UserResponse(Long id, String username, String fullName, String role, Long departmentId, String departmentName, boolean enabled) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getRole(),
            user.getDepartment() != null ? user.getDepartment().getId() : null,
            user.getDepartment() != null ? user.getDepartment().getName() : null,
            user.isEnabled()
        );
    }
}
