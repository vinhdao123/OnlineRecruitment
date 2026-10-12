package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.admin.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.admin.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public long totalUsers()                        { return userRepository.count(); }
    public long usersByRole(UserRole role)          { return userRepository.countByRole(role); }
    public long usersByStatus(UserStatus status)    { return userRepository.countByStatus(status); }
    public long totalCompanies()                    { return companyRepository.count(); }
    public long companiesByStatus(CompanyStatus s)  { return companyRepository.countByStatus(s); }
    public long totalJobs()                         { return jobRepository.count(); }
    public long jobsByStatus(String status)         { return jobRepository.countByStatus(status); }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> userGrowthLast6Months() {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            LocalDateTime from = ym.atDay(1).atStartOfDay();
            LocalDateTime to   = ym.plusMonths(1).atDay(1).atStartOfDay();

            long count = userRepository.countBetween(from, to);

            Map<String, Object> row = new HashMap<>();
            row.put("label", ym.getMonth().name().substring(0, 3));
            row.put("year",  ym.getYear());
            row.put("month", ym.getMonthValue());
            row.put("count", count);
            result.add(row);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<User> recentUsers(int limit) {
        return userRepository.findRecent(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Company> recentCompanies(int limit) {
        return companyRepository.findRecent(PageRequest.of(0, limit));
    }
}