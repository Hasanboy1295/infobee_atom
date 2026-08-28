package com.infobee.dto;

import com.infobee.model.Code;

public record CodeResponse(Long id, Long groupId, String groupCode, String codeValue, String codeLabel,
                           Integer sortOrder, boolean enabled) {
    public static CodeResponse from(Code c) {
        return new CodeResponse(c.getId(), c.getGroup().getId(), c.getGroup().getGroupCode(),
            c.getCodeValue(), c.getCodeLabel(), c.getSortOrder(), c.isEnabled());
    }
}
