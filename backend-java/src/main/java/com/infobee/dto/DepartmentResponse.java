package com.infobee.dto;

import com.infobee.model.Department;

public record DepartmentResponse(Long id, String name) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(department.getId(), department.getName());
    }
}
