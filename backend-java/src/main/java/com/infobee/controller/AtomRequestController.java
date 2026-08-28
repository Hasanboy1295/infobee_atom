package com.infobee.controller;

import com.infobee.dto.CommentRequest;
import com.infobee.dto.CommentResponse;
import com.infobee.dto.HistoryResponse;
import com.infobee.dto.RequestCreate;
import com.infobee.dto.RequestFilter;
import com.infobee.dto.RequestPageResponse;
import com.infobee.dto.RequestResponse;
import com.infobee.dto.RequestUpdate;
import com.infobee.dto.TransitionRequest;
import com.infobee.model.RequestType;
import com.infobee.service.RequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;

@RestController
@RequestMapping("/api/atom-requests")
@Tag(name = "ATOM requests", description = "Authenticated ATOM request workflows, comments, and history")
@SecurityRequirement(name = "bearerAuth")
public class AtomRequestController {
    private final RequestService service;
    public AtomRequestController(RequestService service) { this.service = service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create ATOM request", description = "Requires authentication; the authenticated user becomes the owner.")
    public RequestResponse create(@Valid @RequestBody RequestCreate input, Authentication auth) {
        return service.create(RequestType.ATOM, input, auth);
    }
    @GetMapping
    @Operation(summary = "List ATOM requests", description = "Requires authentication. Users see their own requests; ADMIN sees all. Supports filtering by status, priority, departmentId, date range, and search.")
    public RequestPageResponse<RequestResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt") String sort,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) String createdFrom,
        @RequestParam(required = false) String createdTo,
        @RequestParam(required = false) String search,
        Authentication auth
    ) {
        RequestFilter filter = new RequestFilter(status, priority, departmentId, createdFrom, createdTo, search);
        return service.list(RequestType.ATOM, toPageable(page, size, sort), auth, filter);
    }
    @GetMapping("/{id}")
    @Operation(summary = "Get ATOM request", description = "Requires authentication and access to the request.")
    public RequestResponse get(@PathVariable Long id, Authentication auth) { return service.get(RequestType.ATOM, id, auth); }
    @PutMapping("/{id}")
    @Operation(summary = "Update ATOM request", description = "Requires authentication and ownership/access to the request.")
    public RequestResponse update(@PathVariable Long id, @Valid @RequestBody RequestUpdate input, Authentication auth) {
        return service.update(RequestType.ATOM, id, input, auth);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete ATOM request", description = "Requires authentication and ownership/access to the request.")
    public void delete(@PathVariable Long id, Authentication auth) { service.delete(RequestType.ATOM, id, auth); }
    @PostMapping("/{id}/{action}")
    @Operation(summary = "Transition ATOM request", description = "Requires authentication. Actions are submit, review, approve, reject, and cancel; review/approve/reject require ADMIN.")
    public RequestResponse transition(@PathVariable Long id, @PathVariable String action,
                                      @Valid @RequestBody(required = false) TransitionRequest input, Authentication auth) {
        return service.transition(RequestType.ATOM, id, action, input, auth);
    }
    @PostMapping("/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add ATOM comment", description = "Requires authentication and access to the request.")
    public CommentResponse addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest input, Authentication auth) {
        return service.addComment(RequestType.ATOM, id, input, auth);
    }
    @GetMapping("/{id}/comments")
    @Operation(summary = "List ATOM comments", description = "Requires authentication and access to the request.")
    public List<CommentResponse> comments(@PathVariable Long id, Authentication auth) { return service.comments(RequestType.ATOM, id, auth); }
    @GetMapping("/{id}/history")
    @Operation(summary = "List ATOM history", description = "Requires authentication and access to the request.")
    public List<HistoryResponse> history(@PathVariable Long id, Authentication auth) { return service.history(RequestType.ATOM, id, auth); }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "title", "status", "priority", "createdAt", "updatedAt");

    private Pageable toPageable(int page, int size, String sort) {
        String[] parts = sort.split(",", -1);
        if (parts.length > 2 || parts[0].isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sort parameter");
        }
        String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + field);
        }
        Sort.Direction direction = parts.length == 2
            ? Sort.Direction.fromOptionalString(parts[1]).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sort direction"))
            : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(new Sort.Order(direction, field)));
    }
}
