package com.infobee.service;

import com.infobee.dto.NotificationResponse;
import com.infobee.model.Notification;
import com.infobee.model.User;
import com.infobee.repository.NotificationRepository;
import com.infobee.repository.UserRepository;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final UserRepository userRepo;

    public NotificationService(NotificationRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    public void notify(String username, String type, String title, String message,
                       String entityType, Long entityId) {
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return;
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(String username, int page, int size, boolean unreadOnly) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Notification> notifications = unreadOnly
            ? repo.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId(), pageable)
            : repo.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        return notifications.map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(String username) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        long unread = repo.countByUserIdAndReadFalse(user.getId());
        return Map.of("unreadCount", unread);
    }

    @Transactional
    public void markRead(String username, Long notificationId) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Notification n = repo.findById(notificationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
        repo.save(n);
    }

    @Transactional
    public int markAllRead(String username) {
        User user = userRepo.findByUsername(username)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return repo.markAllAsRead(user.getId());
    }
}
