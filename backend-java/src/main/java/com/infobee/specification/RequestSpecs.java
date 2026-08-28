package com.infobee.specification;

import com.infobee.dto.RequestFilter;
import com.infobee.model.BaseRequest;
import com.infobee.model.RequestPriority;
import com.infobee.model.RequestStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class RequestSpecs {
    private RequestSpecs() {}

    public static <T extends BaseRequest> Specification<T> fromFilter(RequestFilter filter) {
        if (filter == null) return (root, query, cb) -> cb.conjunction();
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null && !filter.status().isBlank()) {
                try {
                    RequestStatus status = RequestStatus.valueOf(filter.status().trim().toUpperCase(Locale.ROOT));
                    predicates.add(cb.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    predicates.add(cb.disjunction());
                }
            }

            if (filter.priority() != null && !filter.priority().isBlank()) {
                try {
                    RequestPriority priority = RequestPriority.valueOf(filter.priority().trim().toUpperCase(Locale.ROOT));
                    predicates.add(cb.equal(root.get("priority"), priority));
                } catch (IllegalArgumentException e) {
                    predicates.add(cb.disjunction());
                }
            }

            if (filter.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), filter.departmentId()));
            }

            if (filter.createdFrom() != null && !filter.createdFrom().isBlank()) {
                try {
                    Instant from = Instant.parse(filter.createdFrom().trim());
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                } catch (DateTimeParseException e) {
                    predicates.add(cb.disjunction());
                }
            }

            if (filter.createdTo() != null && !filter.createdTo().isBlank()) {
                try {
                    Instant to = Instant.parse(filter.createdTo().trim());
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
                } catch (DateTimeParseException e) {
                    predicates.add(cb.disjunction());
                }
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
