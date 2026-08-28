package com.infobee.dto;

import com.infobee.model.CodeGroup;

public record CodeGroupResponse(Long id, String groupCode, String groupName, String description) {
    public static CodeGroupResponse from(CodeGroup g) {
        return new CodeGroupResponse(g.getId(), g.getGroupCode(), g.getGroupName(), g.getDescription());
    }
}
