package com.infobee.controller;

import com.infobee.dto.CodeGroupRequest;
import com.infobee.dto.CodeGroupResponse;
import com.infobee.dto.CodeRequest;
import com.infobee.dto.CodeResponse;
import com.infobee.dto.DepartmentRequest;
import com.infobee.dto.DepartmentResponse;
import com.infobee.dto.AdminPageResponse;
import com.infobee.dto.MenuRequest;
import com.infobee.dto.MenuResponse;
import com.infobee.dto.RoleRequest;
import com.infobee.dto.RoleResponse;
import com.infobee.dto.UserRequest;
import com.infobee.dto.UserResponse;
import com.infobee.model.ActivityLog;
import com.infobee.model.Code;
import com.infobee.model.CodeGroup;
import com.infobee.model.Department;
import com.infobee.model.Menu;
import com.infobee.model.Role;
import com.infobee.model.User;
import com.infobee.repository.CodeGroupRepository;
import com.infobee.repository.CodeRepository;
import com.infobee.repository.DepartmentRepository;
import com.infobee.repository.MenuRepository;
import com.infobee.repository.RoleRepository;
import com.infobee.repository.UserRepository;
import com.infobee.service.ActivityLogService;
import com.infobee.service.UserService;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "ADMIN-only CRUD for users, departments, roles, and menus")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SEARCH_LENGTH = 100;

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final MenuRepository menuRepository;
    private final CodeGroupRepository codeGroupRepository;
    private final CodeRepository codeRepository;
    private final UserService userService;
    private final ActivityLogService activityLogService;

    public AdminController(
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        RoleRepository roleRepository,
        MenuRepository menuRepository,
        CodeGroupRepository codeGroupRepository,
        CodeRepository codeRepository,
        UserService userService,
        ActivityLogService activityLogService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.menuRepository = menuRepository;
        this.codeGroupRepository = codeGroupRepository;
        this.codeRepository = codeRepository;
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Requires ADMIN. Returns a bounded, searchable page without password hashes.")
    public AdminPageResponse<UserResponse> getUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = pageable(page, size, sort, Set.of("id", "username", "fullName", "role", "enabled"));
        Page<User> users = search(search).isEmpty()
            ? userRepository.findAll(pageable)
            : userRepository.findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrRoleContainingIgnoreCase(
                search(search), search(search), search(search), pageable);
        return pageResponse(users, UserResponse::from);
    }

    @PostMapping("/users")
    @Operation(summary = "Create user", description = "Requires ADMIN. Password is accepted for account creation but never returned.")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        User user = userService.createUser(new User(request.username(), request.password(), request.fullName(), request.role()));
        userService.assignDepartment(user, request.departmentId());
        User saved = userRepository.save(user);
        activityLogService.logWithoutActor(ActivityLog.Action.USER_CREATED, null, saved.getId(),
            saved.getUsername(), null);
        return UserResponse.from(saved);
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update user", description = "Requires ADMIN. Replaces user profile and password.")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userService.updateUser(existing, request.username(), request.password(), request.fullName(), request.role());
        userService.assignDepartment(existing, request.departmentId());
        activityLogService.logWithoutActor(ActivityLog.Action.USER_UPDATED, null, id,
            existing.getUsername(), null);
        return UserResponse.from(userRepository.save(existing));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete user", description = "Requires ADMIN.")
    public void deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        activityLogService.logWithoutActor(ActivityLog.Action.USER_UPDATED, null, id,
            "Deleted user #" + id, null);
        userRepository.deleteById(id);
    }

    @PatchMapping("/users/{id}/enabled")
    @Operation(summary = "Enable or disable user", description = "Requires ADMIN. Disabled users cannot log in or use existing JWTs.")
    public UserResponse setUserEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!enabled && "ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The admin account cannot be disabled");
        }
        user.setEnabled(enabled);
        ActivityLog.Action action = enabled ? ActivityLog.Action.USER_ENABLED : ActivityLog.Action.USER_DISABLED;
        activityLogService.logWithoutActor(action, null, id, user.getUsername(), null);
        return UserResponse.from(userRepository.save(user));
    }

    @GetMapping("/departments")
    @Operation(summary = "List departments", description = "Requires ADMIN. Returns a bounded searchable page.")
    public AdminPageResponse<DepartmentResponse> getDepartments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = pageable(page, size, sort, Set.of("id", "name"));
        Page<Department> departments = search(search).isEmpty()
            ? departmentRepository.findAll(pageable)
            : departmentRepository.findByNameContainingIgnoreCase(search(search), pageable);
        return pageResponse(departments, DepartmentResponse::from);
    }

    @PostMapping("/departments")
    @Operation(summary = "Create department", description = "Requires ADMIN.")
    public DepartmentResponse createDepartment(@Valid @RequestBody DepartmentRequest request) {
        if (departmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }
        return DepartmentResponse.from(departmentRepository.save(new Department(request.name().trim())));
    }

    @PutMapping("/departments/{id}")
    @Operation(summary = "Update department", description = "Requires ADMIN.")
    public DepartmentResponse updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        Department existing = departmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
        if (!existing.getName().equalsIgnoreCase(request.name())
            && departmentRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department already exists");
        }
        existing.setName(request.name().trim());
        return DepartmentResponse.from(departmentRepository.save(existing));
    }

    @DeleteMapping("/departments/{id}")
    @Operation(summary = "Delete department", description = "Requires ADMIN.")
    public void deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found");
        }
        departmentRepository.deleteById(id);
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles", description = "Requires ADMIN. Returns a bounded searchable page.")
    public AdminPageResponse<RoleResponse> getRoles(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = pageable(page, size, sort, Set.of("id", "name"));
        Page<Role> roles = search(search).isEmpty()
            ? roleRepository.findAll(pageable)
            : roleRepository.findByNameContainingIgnoreCase(search(search), pageable);
        return pageResponse(roles, RoleResponse::from);
    }

    @PostMapping("/roles")
    @Operation(summary = "Create role", description = "Requires ADMIN.")
    public RoleResponse createRole(@Valid @RequestBody RoleRequest request) {
        if (roleRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }
        return RoleResponse.from(roleRepository.save(new Role(request.name().trim().toUpperCase(java.util.Locale.ROOT))));
    }

    @PutMapping("/roles/{id}")
    @Operation(summary = "Update role", description = "Requires ADMIN.")
    public RoleResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        Role existing = roleRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!existing.getName().equalsIgnoreCase(request.name())
            && roleRepository.existsByNameIgnoreCase(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }
        existing.setName(request.name().trim().toUpperCase(java.util.Locale.ROOT));
        return RoleResponse.from(roleRepository.save(existing));
    }

    @DeleteMapping("/roles/{id}")
    @Operation(summary = "Delete role", description = "Requires ADMIN.")
    public void deleteRole(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
        }
        roleRepository.deleteById(id);
    }

    @GetMapping("/menus")
    @Operation(summary = "List menus", description = "Requires ADMIN. Returns a bounded searchable page.")
    public AdminPageResponse<MenuResponse> getMenus(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = pageable(page, size, sort, Set.of("id", "label", "path"));
        Page<Menu> menus = search(search).isEmpty()
            ? menuRepository.findAll(pageable)
            : menuRepository.findByLabelContainingIgnoreCaseOrPathContainingIgnoreCase(
                search(search), search(search), pageable);
        return pageResponse(menus, MenuResponse::from);
    }

    @PostMapping("/menus")
    @Operation(summary = "Create menu", description = "Requires ADMIN.")
    public MenuResponse createMenu(@Valid @RequestBody MenuRequest request) {
        return MenuResponse.from(menuRepository.save(new Menu(request.label().trim(), request.path().trim())));
    }

    @PutMapping("/menus/{id}")
    @Operation(summary = "Update menu", description = "Requires ADMIN.")
    public MenuResponse updateMenu(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        Menu existing = menuRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found"));
        existing.setLabel(request.label().trim());
        existing.setPath(request.path().trim());
        return MenuResponse.from(menuRepository.save(existing));
    }

    @DeleteMapping("/menus/{id}")
    @Operation(summary = "Delete menu", description = "Requires ADMIN.")
    public void deleteMenu(@PathVariable Long id) {
        if (!menuRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu not found");
        }
        menuRepository.deleteById(id);
    }

    // ── Code Group CRUD ──────────────────────────────────────────────

    @GetMapping("/code-groups")
    @Operation(summary = "List code groups")
    public AdminPageResponse<CodeGroupResponse> getCodeGroups(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "id,asc") String sort,
        @RequestParam(defaultValue = "") String search
    ) {
        Pageable pageable = pageable(page, size, sort, Set.of("id", "groupCode", "groupName"));
        Page<CodeGroup> groups = search(search).isEmpty()
            ? codeGroupRepository.findAll(pageable)
            : codeGroupRepository.findByGroupCodeContainingIgnoreCaseOrGroupNameContainingIgnoreCase(
                search(search), search(search), pageable);
        return pageResponse(groups, CodeGroupResponse::from);
    }

    @PostMapping("/code-groups")
    @Operation(summary = "Create code group")
    public CodeGroupResponse createCodeGroup(@Valid @RequestBody CodeGroupRequest request) {
        if (codeGroupRepository.existsByGroupCodeIgnoreCase(request.groupCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code group already exists");
        }
        return CodeGroupResponse.from(codeGroupRepository.save(
            new CodeGroup(request.groupCode().trim().toUpperCase(), request.groupName().trim(), request.description())));
    }

    @PutMapping("/code-groups/{id}")
    @Operation(summary = "Update code group")
    public CodeGroupResponse updateCodeGroup(@PathVariable Long id, @Valid @RequestBody CodeGroupRequest request) {
        CodeGroup existing = codeGroupRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code group not found"));
        if (!existing.getGroupCode().equalsIgnoreCase(request.groupCode())
            && codeGroupRepository.existsByGroupCodeIgnoreCase(request.groupCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code group already exists");
        }
        existing.setGroupCode(request.groupCode().trim().toUpperCase());
        existing.setGroupName(request.groupName().trim());
        existing.setDescription(request.description());
        return CodeGroupResponse.from(codeGroupRepository.save(existing));
    }

    @DeleteMapping("/code-groups/{id}")
    @Operation(summary = "Delete code group")
    public void deleteCodeGroup(@PathVariable Long id) {
        if (!codeGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code group not found");
        }
        codeGroupRepository.deleteById(id);
    }

    // ── Code CRUD ────────────────────────────────────────────────────

    @GetMapping("/code-groups/{groupId}/codes")
    @Operation(summary = "List codes in a group")
    public AdminPageResponse<CodeResponse> getCodes(
        @PathVariable Long groupId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.ASC, "id")));
        Page<Code> codes = codeRepository.findByGroupIdOrderBySortOrderAsc(groupId, pageable);
        return pageResponse(codes, CodeResponse::from);
    }

    @GetMapping("/codes/by-group/{groupCode}")
    @Operation(summary = "List active codes by group code (public for dropdowns)")
    public java.util.List<CodeResponse> getActiveCodesByGroupCode(@PathVariable String groupCode) {
        return codeRepository.findByGroup_GroupCodeAndEnabledTrueOrderBySortOrderAsc(groupCode)
            .stream().map(CodeResponse::from).toList();
    }

    @PostMapping("/codes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create code")
    public CodeResponse createCode(@Valid @RequestBody CodeRequest request) {
        CodeGroup group = codeGroupRepository.findById(request.groupId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code group not found"));
        if (codeRepository.existsByGroupIdAndCodeValueIgnoreCase(request.groupId(), request.codeValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code value already exists in this group");
        }
        Code code = new Code(group, request.codeValue().trim().toUpperCase(),
            request.codeLabel().trim(), request.sortOrder() != null ? request.sortOrder() : 0);
        return CodeResponse.from(codeRepository.save(code));
    }

    @PutMapping("/codes/{id}")
    @Operation(summary = "Update code")
    public CodeResponse updateCode(@PathVariable Long id, @Valid @RequestBody CodeRequest request) {
        Code existing = codeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found"));
        CodeGroup group = codeGroupRepository.findById(request.groupId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code group not found"));
        if (!existing.getCodeValue().equalsIgnoreCase(request.codeValue())
            && codeRepository.existsByGroupIdAndCodeValueIgnoreCase(request.groupId(), request.codeValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code value already exists in this group");
        }
        existing.setGroup(group);
        existing.setCodeValue(request.codeValue().trim().toUpperCase());
        existing.setCodeLabel(request.codeLabel().trim());
        existing.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        return CodeResponse.from(codeRepository.save(existing));
    }

    @DeleteMapping("/codes/{id}")
    @Operation(summary = "Delete code")
    public void deleteCode(@PathVariable Long id) {
        if (!codeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found");
        }
        codeRepository.deleteById(id);
    }

    @PatchMapping("/codes/{id}/enabled")
    @Operation(summary = "Enable or disable code")
    public CodeResponse setCodeEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        Code code = codeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code not found"));
        code.setEnabled(enabled);
        return CodeResponse.from(codeRepository.save(code));
    }

    private Pageable pageable(int page, int size, String sort, Set<String> supportedFields) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater");
        }
        if (size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be positive");
        }

        String[] parts = sort == null ? new String[0] : sort.trim().split(",", -1);
        if (parts.length < 1 || parts.length > 2 || parts[0].isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sort parameter");
        }
        String field = parts[0].trim();
        if (!supportedFields.contains(field)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + field);
        }
        Sort.Direction direction;
        if (parts.length == 1) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.fromOptionalString(parts[1].trim()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed sort direction"));
        }
        Sort ordering = Sort.by(direction, field);
        if (!field.equals("id")) {
            ordering = ordering.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), ordering);
    }

    private String search(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > MAX_SEARCH_LENGTH || normalized.indexOf('\u0000') >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Search must be at most " + MAX_SEARCH_LENGTH + " characters");
        }
        return normalized;
    }

    private <E, R> AdminPageResponse<R> pageResponse(Page<E> page, Function<E, R> mapper) {
        return new AdminPageResponse<>(
            page.getContent().stream().map(mapper).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
