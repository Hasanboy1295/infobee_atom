package com.infobee.dto;

import com.infobee.model.Menu;

public record MenuResponse(Long id, String label, String path) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getLabel(), menu.getPath());
    }
}
