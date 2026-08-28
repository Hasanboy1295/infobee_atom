package com.infobee.controller;

import com.infobee.dto.BatchResult;
import com.infobee.dto.BatchTransitionRequest;
import com.infobee.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch operations", description = "Bulk approve/reject/delete requests")
@SecurityRequirement(name = "bearerAuth")
public class BatchController {
    private final BatchService batchService;

    public BatchController(BatchService batchService) { this.batchService = batchService; }

    @PostMapping("/atom/transition")
    @Operation(summary = "Bulk transition ATOM requests (approve/reject/cancel)")
    public BatchResult batchTransitionAtom(@Valid @RequestBody BatchTransitionRequest req, Authentication auth) {
        return batchService.batchTransition("ATOM", req, auth);
    }

    @PostMapping("/cpsr/transition")
    @Operation(summary = "Bulk transition CPSR requests (approve/reject/cancel)")
    public BatchResult batchTransitionCpsr(@Valid @RequestBody BatchTransitionRequest req, Authentication auth) {
        return batchService.batchTransition("CPSR", req, auth);
    }

    @PostMapping("/atom/delete")
    @Operation(summary = "Bulk delete ATOM requests (owner only, draft/rejected only)")
    public BatchResult batchDeleteAtom(@RequestBody List<Long> ids, Authentication auth) {
        return batchService.batchDelete("ATOM", ids, auth);
    }

    @PostMapping("/cpsr/delete")
    @Operation(summary = "Bulk delete CPSR requests (owner only, draft/rejected only)")
    public BatchResult batchDeleteCpsr(@RequestBody List<Long> ids, Authentication auth) {
        return batchService.batchDelete("CPSR", ids, auth);
    }
}
