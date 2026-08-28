package com.infobee.service;

import com.infobee.dto.CommentRequest;
import com.infobee.dto.CommentResponse;
import com.infobee.dto.HistoryResponse;
import com.infobee.dto.RequestCreate;
import com.infobee.dto.RequestFilter;
import com.infobee.dto.RequestPageResponse;
import com.infobee.dto.RequestResponse;
import com.infobee.dto.RequestUpdate;
import com.infobee.dto.TransitionRequest;
import com.infobee.model.AtomRequest;
import com.infobee.model.BaseRequest;
import com.infobee.model.CpsrRequest;
import com.infobee.model.Department;
import com.infobee.model.RequestComment;
import com.infobee.model.RequestHistory;
import com.infobee.model.RequestPriority;
import com.infobee.model.RequestStatus;
import com.infobee.model.RequestType;
import com.infobee.model.User;
import com.infobee.model.ActivityLog;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.DepartmentRepository;
import com.infobee.repository.RequestCommentRepository;
import com.infobee.repository.RequestHistoryRepository;
import com.infobee.repository.UserRepository;
import com.infobee.specification.RequestSpecs;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RequestService {
    private final AtomRequestRepository atomRepository;
    private final CpsrRequestRepository cpsrRepository;
    private final RequestCommentRepository commentRepository;
    private final RequestHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public RequestService(AtomRequestRepository atomRepository, CpsrRequestRepository cpsrRepository,
                          RequestCommentRepository commentRepository, RequestHistoryRepository historyRepository,
                          UserRepository userRepository, DepartmentRepository departmentRepository,
                          ActivityLogService activityLogService, NotificationService notificationService) {
        this.atomRepository = atomRepository;
        this.cpsrRepository = cpsrRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    private User actor(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
    private boolean admin(Authentication a) {
        return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"));
    }
    private Pageable safePage(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be positive");
        }
        for (Sort.Order order : pageable.getSort()) {
            if (!Set.of("id", "title", "status", "priority", "createdAt", "updatedAt").contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field: " + order.getProperty());
            }
        }
        return PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), 100), pageable.getSort());
    }
    private void checkAccess(User owner, Authentication authentication) {
        if (admin(authentication)) return;
        if (owner.getUsername().equals(authentication.getName())) return;
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (currentUser != null && currentUser.getDepartment() != null && owner.getDepartment() != null
            && currentUser.getDepartment().getId().equals(owner.getDepartment().getId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this request");
    }
    private void checkEditable(RequestStatus status) {
        if (status != RequestStatus.DRAFT && status != RequestStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft or rejected requests can be edited");
        }
    }

    private void applyCommonFields(BaseRequest request, RequestCreate input) {
        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        if (input.departmentId() != null) {
            Department dept = departmentRepository.findById(input.departmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            request.setDepartment(dept);
        }
        if (input.priority() != null) {
            try {
                request.setPriority(RequestPriority.valueOf(input.priority().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority value");
            }
        }
        if (input.dueDate() != null && !input.dueDate().isBlank()) {
            try {
                request.setDueDate(Instant.parse(input.dueDate().trim()));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dueDate format, use ISO-8601");
            }
        }
        if (input.tags() != null) {
            request.setTags(input.tags().trim());
        }
    }

    private void applyCommonFields(BaseRequest request, RequestUpdate input) {
        request.setTitle(input.title().trim());
        request.setDescription(input.description().trim());
        if (input.departmentId() != null) {
            Department dept = departmentRepository.findById(input.departmentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department not found"));
            request.setDepartment(dept);
        }
        if (input.priority() != null) {
            try {
                request.setPriority(RequestPriority.valueOf(input.priority().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid priority value");
            }
        }
        if (input.dueDate() != null && !input.dueDate().isBlank()) {
            try {
                request.setDueDate(Instant.parse(input.dueDate().trim()));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dueDate format, use ISO-8601");
            }
        }
        if (input.tags() != null) {
            request.setTags(input.tags().trim());
        }
    }

    private void applyCpsrFields(CpsrRequest request, RequestCreate input) {
        if (input.requesterName() != null) request.setRequesterName(input.requesterName().trim());
        if (input.requesterEmail() != null) request.setRequesterEmail(input.requesterEmail().trim());
        if (input.requesterPhone() != null) request.setRequesterPhone(input.requesterPhone().trim());
        if (input.companyName() != null) request.setCompanyName(input.companyName().trim());
        if (input.productName() != null) request.setProductName(input.productName().trim());
        if (input.regulatoryFramework() != null) request.setRegulatoryFramework(input.regulatoryFramework().trim());
        if (input.targetMarket() != null) request.setTargetMarket(input.targetMarket().trim());
        if (input.additionalInfo() != null) request.setAdditionalInfo(input.additionalInfo().trim());
    }

    private void applyCpsrFields(CpsrRequest request, RequestUpdate input) {
        if (input.requesterName() != null) request.setRequesterName(input.requesterName().trim());
        if (input.requesterEmail() != null) request.setRequesterEmail(input.requesterEmail().trim());
        if (input.requesterPhone() != null) request.setRequesterPhone(input.requesterPhone().trim());
        if (input.companyName() != null) request.setCompanyName(input.companyName().trim());
        if (input.productName() != null) request.setProductName(input.productName().trim());
        if (input.regulatoryFramework() != null) request.setRegulatoryFramework(input.regulatoryFramework().trim());
        if (input.targetMarket() != null) request.setTargetMarket(input.targetMarket().trim());
        if (input.additionalInfo() != null) request.setAdditionalInfo(input.additionalInfo().trim());
    }

    @Transactional
    public RequestResponse create(RequestType type, RequestCreate input, Authentication authentication) {
        User owner = actor(authentication);
        RequestResponse result;
        if (type == RequestType.ATOM) {
            AtomRequest request = new AtomRequest();
            request.setOwner(owner);
            applyCommonFields(request, input);
            result = RequestResponse.from(atomRepository.save(request), "ATOM");
        } else {
            CpsrRequest request = new CpsrRequest();
            request.setOwner(owner);
            applyCommonFields(request, input);
            applyCpsrFields(request, input);
            result = RequestResponse.from(cpsrRepository.save(request), "CPSR");
        }
        activityLogService.log(owner, ActivityLog.Action.REQUEST_CREATED, type, result.id(), result.title(), null);
        if (type == RequestType.CPSR) {
            notificationService.notify(owner.getUsername(), "REQUEST_CREATED",
                "New CPSR Request", "Your CPSR request '" + result.title() + "' has been created.",
                "CPSR", result.id());
        } else {
            notificationService.notify(owner.getUsername(), "REQUEST_CREATED",
                "New ATOM Request", "Your ATOM request '" + result.title() + "' has been created.",
                "ATOM", result.id());
        }
        return result;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public RequestPageResponse<RequestResponse> list(
        RequestType type,
        Pageable pageable,
        Authentication authentication,
        RequestFilter filter
    ) {
        Pageable safePageable = safePage(pageable);
        User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
        Long departmentId = currentUser != null && currentUser.getDepartment() != null
            ? currentUser.getDepartment().getId() : null;

        Specification<BaseRequest> accessSpec = (Specification<BaseRequest>)(Specification<?>) buildAccessSpec(authentication, departmentId);
        Specification<BaseRequest> filterSpec = (Specification<BaseRequest>)(Specification<?>) RequestSpecs.fromFilter(filter);
        Specification<BaseRequest> combined = accessSpec.and(filterSpec);

        Page<RequestResponse> responsePage;

        if (type == RequestType.ATOM) {
            Page<AtomRequest> page = atomRepository.findAll((Specification<AtomRequest>)(Specification<?>) combined, safePageable);
            responsePage = page.map(r -> RequestResponse.from(r, "ATOM"));
        } else {
            Page<CpsrRequest> page = cpsrRepository.findAll((Specification<CpsrRequest>)(Specification<?>) combined, safePageable);
            responsePage = page.map(r -> RequestResponse.from(r, "CPSR"));
        }
        return new RequestPageResponse<>(
            responsePage.getContent(),
            responsePage.getNumber(),
            responsePage.getSize(),
            responsePage.getTotalElements(),
            responsePage.getTotalPages()
        );
    }

    @SuppressWarnings("unchecked")
    private Specification<? extends BaseRequest> buildAccessSpec(Authentication authentication, Long departmentId) {
        return (Specification<BaseRequest>) (root, query, cb) -> {
            if (admin(authentication)) return cb.conjunction();
            if (departmentId != null) {
                return cb.or(
                    cb.equal(root.get("owner").get("username"), authentication.getName()),
                    cb.equal(root.get("owner").get("department").get("id"), departmentId)
                );
            }
            return cb.equal(root.get("owner").get("username"), authentication.getName());
        };
    }

    @Transactional(readOnly = true)
    public RequestResponse get(RequestType type, Long id, Authentication authentication) {
        if (type == RequestType.ATOM) {
            AtomRequest request = atomRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            return RequestResponse.from(request, "ATOM");
        }
        CpsrRequest request = cpsrRepository.findById(id).orElseThrow(() -> notFound(type));
        checkAccess(request.getOwner(), authentication);
        return RequestResponse.from(request, "CPSR");
    }

    @Transactional
    public RequestResponse update(RequestType type, Long id, RequestUpdate input, Authentication authentication) {
        User user = actor(authentication);
        RequestResponse result;
        if (type == RequestType.ATOM) {
            AtomRequest request = atomRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            checkEditable(request.getStatus());
            applyCommonFields(request, input);
            result = RequestResponse.from(request, "ATOM");
        } else {
            CpsrRequest request = cpsrRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            checkEditable(request.getStatus());
            applyCommonFields(request, input);
            applyCpsrFields(request, input);
            result = RequestResponse.from(request, "CPSR");
        }
        activityLogService.log(user, ActivityLog.Action.REQUEST_UPDATED, type, id, result.title(), null);
        return result;
    }

    @Transactional
    public void delete(RequestType type, Long id, Authentication authentication) {
        User user = actor(authentication);
        if (type == RequestType.ATOM) {
            AtomRequest request = atomRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            checkEditable(request.getStatus());
            atomRepository.delete(request);
        } else {
            CpsrRequest request = cpsrRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            checkEditable(request.getStatus());
            cpsrRepository.delete(request);
        }
        activityLogService.log(user, ActivityLog.Action.REQUEST_DELETED, type, id, "Deleted " + type.name() + " #" + id, null);
    }

    @Transactional
    public RequestResponse transition(RequestType type, Long id, String action, TransitionRequest input,
                                      Authentication authentication) {
        User user = actor(authentication);
        if (action == null || action.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transition action is required");
        }
        String normalized = action.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SUBMIT", "REVIEW", "APPROVE", "REJECT", "CANCEL").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown transition action");
        }
        if (type == RequestType.ATOM) {
            AtomRequest request = atomRepository.findById(id).orElseThrow(() -> notFound(type));
            transition(request.getStatus(), normalized, request.getOwner(), authentication);
            RequestStatus next = next(normalized);
            record(type, id, user, request.getStatus(), next, input == null ? null : input.note());
            request.setStatus(next);
            activityLogService.log(user, ActivityLog.Action.REQUEST_TRANSITIONED, type, id,
                normalized + " -> " + next.name(), null);
            notificationService.notify(request.getOwner().getUsername(), "REQUEST_TRANSITIONED",
                type.name() + " Request " + next.name(),
                "Your " + type.name() + " request #" + id + " has been " + normalized.toLowerCase() + ".",
                type.name(), id);
            return RequestResponse.from(request, "ATOM");
        }
        CpsrRequest request = cpsrRepository.findById(id).orElseThrow(() -> notFound(type));
        transition(request.getStatus(), normalized, request.getOwner(), authentication);
        RequestStatus next = next(normalized);
        record(type, id, user, request.getStatus(), next, input == null ? null : input.note());
        request.setStatus(next);
        activityLogService.log(user, ActivityLog.Action.REQUEST_TRANSITIONED, type, id,
            normalized + " -> " + next.name(), null);
        notificationService.notify(request.getOwner().getUsername(), "REQUEST_TRANSITIONED",
            type.name() + " Request " + next.name(),
            "Your " + type.name() + " request #" + id + " has been " + normalized.toLowerCase() + ".",
            type.name(), id);
        return RequestResponse.from(request, "CPSR");
    }

    private void transition(RequestStatus current, String action, User owner, Authentication authentication) {
        boolean isAdmin = admin(authentication);
        if ("SUBMIT".equals(action) && !isAdmin && !owner.getUsername().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can submit this request");
        }
        if (("APPROVE".equals(action) || "REJECT".equals(action) || "REVIEW".equals(action)) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an administrator can review this request");
        }
        boolean valid = switch (action) {
            case "SUBMIT" -> current == RequestStatus.DRAFT || current == RequestStatus.REJECTED;
            case "REVIEW" -> current == RequestStatus.SUBMITTED;
            case "APPROVE", "REJECT" -> current == RequestStatus.SUBMITTED || current == RequestStatus.UNDER_REVIEW;
            case "CANCEL" -> current != RequestStatus.APPROVED && current != RequestStatus.CANCELLED;
            default -> false;
        };
        if (!valid) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition");
        if ("CANCEL".equals(action) && !isAdmin && !owner.getUsername().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the owner can cancel this request");
        }
    }

    private RequestStatus next(String action) {
        return switch (action) {
            case "SUBMIT" -> RequestStatus.SUBMITTED;
            case "REVIEW" -> RequestStatus.UNDER_REVIEW;
            case "APPROVE" -> RequestStatus.APPROVED;
            case "REJECT" -> RequestStatus.REJECTED;
            case "CANCEL" -> RequestStatus.CANCELLED;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown transition");
        };
    }

    private void record(RequestType type, Long id, User actor, RequestStatus from, RequestStatus to, String note) {
        historyRepository.save(new RequestHistory(type, id, actor, from, to, note));
    }

    private ResponseStatusException notFound(RequestType type) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type.name() + " request not found");
    }

    @Transactional
    public CommentResponse addComment(RequestType type, Long id, CommentRequest input, Authentication authentication) {
        User author = actor(authentication);
        RequestComment comment = new RequestComment();
        if (type == RequestType.ATOM) {
            AtomRequest request = atomRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            comment.setAtomRequest(request);
        } else {
            CpsrRequest request = cpsrRepository.findById(id).orElseThrow(() -> notFound(type));
            checkAccess(request.getOwner(), authentication);
            comment.setCpsrRequest(request);
        }
        comment.setAuthor(author);
        comment.setBody(input.body().trim());
        CommentResponse result = CommentResponse.from(commentRepository.save(comment));
        activityLogService.log(author, ActivityLog.Action.COMMENT_ADDED, type, id, result.body(), null);
        String ownerUsername = type == RequestType.ATOM
            ? atomRepository.findById(id).map(r -> r.getOwner().getUsername()).orElse(null)
            : cpsrRepository.findById(id).map(r -> r.getOwner().getUsername()).orElse(null);
        if (ownerUsername != null && !ownerUsername.equals(author.getUsername())) {
            notificationService.notify(ownerUsername, "COMMENT_ADDED", "New Comment",
                author.getUsername() + " commented on " + type.name() + " request #" + id,
                type.name(), id);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(RequestType type, Long id, Authentication authentication) {
        get(type, id, authentication);
        List<RequestComment> comments = type == RequestType.ATOM
            ? commentRepository.findByAtomRequestIdOrderByCreatedAtAsc(id)
            : commentRepository.findByCpsrRequestIdOrderByCreatedAtAsc(id);
        return comments.stream().map(CommentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> history(RequestType type, Long id, Authentication authentication) {
        get(type, id, authentication);
        return historyRepository.findByRequestTypeAndRequestIdOrderByCreatedAtAsc(type, id).stream()
            .map(HistoryResponse::from).toList();
    }
}
