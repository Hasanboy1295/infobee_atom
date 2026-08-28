package com.infobee.controller;

import com.infobee.dto.RequestFilter;
import com.infobee.dto.RequestResponse;
import com.infobee.model.RequestType;
import com.infobee.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@Tag(name = "Export", description = "Export requests as JSON")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) { this.exportService = exportService; }

    @GetMapping("/atom")
    @Operation(summary = "Export ATOM requests as JSON array (filtered)")
    public Map<String, Object> exportAtom(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) String search,
            Authentication auth) {
        RequestFilter filter = new RequestFilter(status, priority, departmentId, createdFrom, createdTo, search);
        List<RequestResponse> data = exportService.exportRequests(RequestType.ATOM, filter, auth);
        return Map.of("type", "ATOM", "count", data.size(), "data", data);
    }

    @GetMapping("/cpsr")
    @Operation(summary = "Export CPSR requests as JSON array (filtered)")
    public Map<String, Object> exportCpsr(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) String search,
            Authentication auth) {
        RequestFilter filter = new RequestFilter(status, priority, departmentId, createdFrom, createdTo, search);
        List<RequestResponse> data = exportService.exportRequests(RequestType.CPSR, filter, auth);
        return Map.of("type", "CPSR", "count", data.size(), "data", data);
    }
}
