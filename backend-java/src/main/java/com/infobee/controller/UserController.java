package com.infobee.controller;

import com.infobee.dto.UserRequest;
import com.infobee.dto.UserResponse;
import com.infobee.model.User;
import jakarta.validation.Valid;
import com.infobee.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Users", description = "ADMIN-only user directory operations")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    @Operation(summary = "List users", description = "Requires ADMIN.")
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/users")
    @Operation(summary = "Create user", description = "Requires ADMIN. Password is never returned.")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return UserResponse.from(userService.createUser(
            new User(request.username(), request.password(), request.fullName(), request.role())
        ));
    }
}
