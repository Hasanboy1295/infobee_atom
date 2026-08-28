package com.infobee.service;

import com.infobee.dto.RequestFilter;
import com.infobee.dto.RequestResponse;
import com.infobee.model.BaseRequest;
import com.infobee.model.RequestType;
import com.infobee.model.User;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.UserRepository;
import com.infobee.specification.RequestSpecs;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ExportService {
    private static final int MAX_EXPORT_ROWS = 10_000;

    private final AtomRequestRepository atomRepo;
    private final CpsrRequestRepository cpsrRepo;
    private final UserRepository userRepo;

    public ExportService(AtomRequestRepository atomRepo, CpsrRequestRepository cpsrRepo, UserRepository userRepo) {
        this.atomRepo = atomRepo;
        this.cpsrRepo = cpsrRepo;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<RequestResponse> exportRequests(RequestType type, RequestFilter filter, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        User currentUser = userRepo.findByUsername(auth.getName()).orElse(null);
        Long departmentId = currentUser != null && currentUser.getDepartment() != null
            ? currentUser.getDepartment().getId() : null;

        Specification<BaseRequest> accessSpec = (Specification<BaseRequest>)(Specification<?>) (root, query, cb) -> {
            if (isAdmin) return cb.conjunction();
            if (departmentId != null) {
                return cb.or(
                    cb.equal(root.get("owner").get("username"), auth.getName()),
                    cb.equal(root.get("owner").get("department").get("id"), departmentId)
                );
            }
            return cb.equal(root.get("owner").get("username"), auth.getName());
        };

        Specification<BaseRequest> filterSpec = (Specification<BaseRequest>)(Specification<?>) RequestSpecs.fromFilter(filter);
        Specification<BaseRequest> combined = accessSpec.and(filterSpec);

        if (type == RequestType.ATOM) {
            var page = atomRepo.findAll(
                (Specification<com.infobee.model.AtomRequest>)(Specification<?>) combined,
                PageRequest.of(0, MAX_EXPORT_ROWS));
            if (page.getTotalElements() > MAX_EXPORT_ROWS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Export limited to " + MAX_EXPORT_ROWS + " records. Use filters to narrow results.");
            }
            return page.getContent().stream().map(r -> RequestResponse.from(r, "ATOM")).toList();
        } else {
            var page = cpsrRepo.findAll(
                (Specification<com.infobee.model.CpsrRequest>)(Specification<?>) combined,
                PageRequest.of(0, MAX_EXPORT_ROWS));
            if (page.getTotalElements() > MAX_EXPORT_ROWS) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Export limited to " + MAX_EXPORT_ROWS + " records. Use filters to narrow results.");
            }
            return page.getContent().stream().map(r -> RequestResponse.from(r, "CPSR")).toList();
        }
    }
}
