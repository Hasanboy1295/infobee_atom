package com.infobee.controller;

import com.infobee.dto.ToxicityRecordRequest;
import com.infobee.dto.ToxicityRecordResponse;
import com.infobee.model.ToxicityRecord;
import com.infobee.repository.ToxicityRecordRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/toxicity-records")
@Tag(name = "Toxicity records", description = "Toxicity DB data (PubChem, ECHA, ToxCast)")
@SecurityRequirement(name = "bearerAuth")
public class ToxicityController {
    private final ToxicityRecordRepository repo;
    private final com.infobee.service.ToxicityLookupService lookupService;

    public ToxicityController(ToxicityRecordRepository repo,
                              com.infobee.service.ToxicityLookupService lookupService) {
        this.repo = repo;
        this.lookupService = lookupService;
    }

    @GetMapping("/lookup")
    @Operation(summary = "Resolve substance hazard data: local cache first, then live PubChem GHS lookup")
    public com.infobee.dto.ToxicityLookupResponse lookup(
            @RequestParam(required = false) String casNumber,
            @RequestParam(required = false) String substanceName) {
        if ((casNumber == null || casNumber.isBlank()) && (substanceName == null || substanceName.isBlank())) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Provide casNumber or substanceName");
        }
        return lookupService.lookup(casNumber, substanceName);
    }

    @GetMapping
    @Operation(summary = "Search toxicity records by CAS or substance name")
    public List<ToxicityRecordResponse> search(
            @RequestParam(required = false) String casNumber,
            @RequestParam(required = false) String substanceName) {
        if (casNumber != null && !casNumber.isBlank()) {
            return repo.findByCasNumber(casNumber).stream().map(ToxicityRecordResponse::from).toList();
        }
        if (substanceName != null && !substanceName.isBlank()) {
            return repo.findBySubstanceNameContainingIgnoreCase(substanceName).stream().map(ToxicityRecordResponse::from).toList();
        }
        return List.of();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get toxicity record by ID")
    public ToxicityRecordResponse get(@PathVariable Long id) {
        return ToxicityRecordResponse.from(repo.findById(id)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Toxicity record not found")));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create toxicity record (from external DB sync or manual entry)")
    public ToxicityRecordResponse create(@Valid @RequestBody ToxicityRecordRequest req,
                                         org.springframework.security.core.Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only administrators can create toxicity records");
        }
        ToxicityRecord rec = new ToxicityRecord();
        rec.setSubstanceName(req.substanceName());
        rec.setCasNumber(req.casNumber());
        rec.setEcNumber(req.ecNumber());
        rec.setSourceDb(req.sourceDb());
        rec.setSourceId(req.sourceId());
        rec.setEndpointName(req.endpointName());
        rec.setEndpointValue(req.endpointValue());
        rec.setEndpointUnit(req.endpointUnit());
        rec.setTestGuideline(req.testGuideline());
        rec.setTestMethod(req.testMethod());
        rec.setRemarks(req.remarks());
        return ToxicityRecordResponse.from(repo.save(rec));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete toxicity record")
    public void delete(@PathVariable Long id,
                       org.springframework.security.core.Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "Only administrators can delete toxicity records");
        }
        if (!repo.existsById(id)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
        }
        repo.deleteById(id);
    }
}
