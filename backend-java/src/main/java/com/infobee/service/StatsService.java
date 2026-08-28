package com.infobee.service;

import com.infobee.dto.DashboardStats;
import com.infobee.model.RequestStatus;
import com.infobee.repository.AtomRequestRepository;
import com.infobee.repository.CpsrRequestRepository;
import com.infobee.repository.DepartmentRepository;
import com.infobee.repository.RequestCommentRepository;
import com.infobee.repository.RequestHistoryRepository;
import com.infobee.repository.RoleRepository;
import com.infobee.repository.UserRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final AtomRequestRepository atomRepository;
    private final CpsrRequestRepository cpsrRepository;
    private final RequestCommentRepository commentRepository;
    private final RequestHistoryRepository historyRepository;

    public StatsService(
        UserRepository userRepository,
        DepartmentRepository departmentRepository,
        RoleRepository roleRepository,
        AtomRequestRepository atomRepository,
        CpsrRequestRepository cpsrRepository,
        RequestCommentRepository commentRepository,
        RequestHistoryRepository historyRepository
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.atomRepository = atomRepository;
        this.cpsrRepository = cpsrRepository;
        this.commentRepository = commentRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long totalDepartments = departmentRepository.count();
        long totalRoles = roleRepository.count();

        long totalAtomRequests = atomRepository.count();
        long totalCpsrRequests = cpsrRepository.count();

        Map<String, Long> atomByStatus = countByStatus(atomRepository.countByStatus());
        Map<String, Long> cpsrByStatus = countByStatus(cpsrRepository.countByStatus());

        long totalComments = commentRepository.count();
        long totalHistoryEntries = historyRepository.count();

        return new DashboardStats(
            totalUsers,
            activeUsers,
            totalDepartments,
            totalRoles,
            totalAtomRequests,
            totalCpsrRequests,
            atomByStatus,
            cpsrByStatus,
            totalComments,
            totalHistoryEntries
        );
    }

    private Map<String, Long> countByStatus(java.util.List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        Arrays.stream(RequestStatus.values()).forEach(s -> result.put(s.name(), 0L));
        for (Object[] row : rows) {
            if (row[0] instanceof RequestStatus status && row[1] instanceof Long count) {
                result.put(status.name(), count);
            }
        }
        return result;
    }
}
