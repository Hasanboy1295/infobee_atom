package com.infobee.config;

import com.infobee.model.User;
import com.infobee.repository.RoleRepository;
import com.infobee.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("postgres")
public class AdminBootstrapRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrapRunner(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        @Value("${ADMIN_BOOTSTRAP_USERNAME:}") String username,
        @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                "ADMIN_BOOTSTRAP_USERNAME and ADMIN_BOOTSTRAP_PASSWORD must be provided together");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters");
        }
        String normalizedUsername = username.trim();
        if (normalizedUsername.isBlank()) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_USERNAME must not be blank");
        }
        if (!roleRepository.existsByNameIgnoreCase("ADMIN")) {
            throw new IllegalStateException("ADMIN role is unavailable; database migrations may be incomplete");
        }
        User admin = userRepository.findByUsername(normalizedUsername).orElseGet(() -> new User(
            normalizedUsername,
            passwordEncoder.encode(password),
            "Initial Administrator",
            "ADMIN"
        ));
        admin.setEnabled(true);
        admin.setPassword(passwordEncoder.encode(password));
        userRepository.save(admin);
    }
}
