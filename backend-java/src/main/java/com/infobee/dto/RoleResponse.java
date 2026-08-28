package com.infobee.dto;

import com.infobee.model.Role;

public record RoleResponse(Long id, String name) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(role.getId(), role.getName());
    }
}
