package com.infobee.service;

import com.infobee.dto.BatchResult;
import com.infobee.dto.BatchTransitionRequest;
import com.infobee.model.AtomRequest;
import com.infobee.model.BaseRequest;
import com.infobee.model.CpsrRequest;
import com.infobee.model.RequestStatus;
import com.infobee.model.User;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BatchService {
    private final AtomRequestRepository atomRepo;
    private final CpsrRequestRepository cpsrRepo;
    private final UserRepository userRepo;

    public BatchService(AtomRequestRepository atomRepo, CpsrRequestRepository cpsrRepo, UserRepository userRepo) {
        this.atomRepo = atomRepo;
        this.cpsrRepo = cpsrRepo;
        this.userRepo = userRepo;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Transactional
    public BatchResult batchTransition(String type, BatchTransitionRequest req, Authentication auth) {
        if (!isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required for batch transitions");
        }
        String action = req.action().trim().toUpperCase(Locale.ROOT);
        if (!Set.of("APPROVE", "REJECT", "CANCEL").contains(action)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action must be APPROVE, REJECT, or CANCEL");
        }

        List<BatchResult.BatchResultItem> details = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (Long id : req.ids()) {
            try {
                BaseRequest request;
                if ("ATOM".equals(type)) {
                    request = atomRepo.findById(id).orElseThrow();
                } else {
                    request = cpsrRepo.findById(id).orElseThrow();
                }
                RequestStatus next = switch (action) {
                    case "APPROVE" -> RequestStatus.APPROVED;
                    case "REJECT" -> RequestStatus.REJECTED;
                    case "CANCEL" -> RequestStatus.CANCELLED;
                    default -> throw new IllegalStateException();
                };
                request.setStatus(next);
                if ("ATOM".equals(type)) {
                    atomRepo.save((AtomRequest) request);
                } else {
                    cpsrRepo.save((CpsrRequest) request);
                }
                succeeded++;
                details.add(new BatchResult.BatchResultItem(id, true, action + " succeeded"));
            } catch (Exception e) {
                failed++;
                details.add(new BatchResult.BatchResultItem(id, false, e.getMessage()));
            }
        }
        return new BatchResult(succeeded, failed, details);
    }

    @Transactional
    public BatchResult batchDelete(String type, List<Long> ids, Authentication auth) {
        User user = userRepo.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        boolean admin = isAdmin(auth);

        List<BatchResult.BatchResultItem> details = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;

        for (Long id : ids) {
            try {
                BaseRequest request;
                if ("ATOM".equals(type)) {
                    request = atomRepo.findById(id).orElseThrow();
                } else {
                    request = cpsrRepo.findById(id).orElseThrow();
                }
                if (!admin && !request.getOwner().getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request");
                }
                if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.REJECTED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft/rejected can be deleted");
                }
                if ("ATOM".equals(type)) {
                    atomRepo.delete((AtomRequest) request);
                } else {
                    cpsrRepo.delete((CpsrRequest) request);
                }
                succeeded++;
                details.add(new BatchResult.BatchResultItem(id, true, "Deleted"));
            } catch (Exception e) {
                failed++;
                details.add(new BatchResult.BatchResultItem(id, false, e.getMessage()));
            }
        }
        return new BatchResult(succeeded, failed, details);
    }
}
