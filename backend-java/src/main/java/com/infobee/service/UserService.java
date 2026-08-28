package com.infobee.service;

import com.infobee.model.Department;
import com.infobee.model.User;
import com.infobee.repository.DepartmentRepository;
import com.infobee.repository.RoleRepository;
import com.infobee.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User createUser(User user) {
        user.setUsername(user.getUsername().trim());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        validateRole(user.getRole());
        user.setRole(normalizeRole(user.getRole()));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User signup(String username, String password, String fullName) {
        return createUser(new User(username, password, fullName.trim(), "USER"));
    }

    public User updateUser(User existing, String username, String password, String fullName, String role) {
        username = username.trim();
        if (!existing.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        validateRole(role);
        existing.setUsername(username);
        existing.setPassword(passwordEncoder.encode(password));
        existing.setFullName(fullName);
        existing.setRole(normalizeRole(role));
        return userRepository.save(existing);
    }

    public void assignDepartment(User user, Long departmentId) {
        if (departmentId == null) {
            user.setDepartment(null);
        } else {
            Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            user.setDepartment(dept);
        }
    }

    private void validateRole(String role) {
        if (role == null || !roleRepository.existsByNameIgnoreCase(role.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role");
        }
    }

    private String normalizeRole(String role) {
        return role.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
