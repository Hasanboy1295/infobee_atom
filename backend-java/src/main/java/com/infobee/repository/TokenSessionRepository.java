package com.infobee.repository;

import com.infobee.model.TokenSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenSessionRepository extends JpaRepository<TokenSession, Long> {
    Optional<TokenSession> findByJtiAndUsername(String jti, String username);
}
