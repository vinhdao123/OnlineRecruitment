
package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.ActivityLog;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;

import OnlineRecruitment.OnlineRecruitment.service.ActivityLogService;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.serviceJob.AdminUserService;
import OnlineRecruitment.OnlineRecruitment.service.serviceJob.JobService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminUserService userService;
    private final CompanyService companyService;
    private final JobService jobService;
    private final ActivityLogService activityLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        // ===== KPI =====
        model.addAttribute("totalUsers", userService.countAll());
        model.addAttribute("totalCompanies", companyService.countAll());
        model.addAttribute("totalJobs", jobService.totalJobs());

        long companyPending =
                companyService.countByStatus(CompanyStatus.PENDING);

        long userLocked =
                userService.countByStatus(UserStatus.LOCKED);

        long jobPending =
                jobService.jobsByStatus(JobStatus.PENDING);

        model.addAttribute("companyPending", companyPending);
        model.addAttribute("userLocked", userLocked);
        model.addAttribute("jobPending", jobPending);
        model.addAttribute(
                "totalPending",
                companyPending + userLocked + jobPending
        );

        // ===== USER ROLES =====
        model.addAttribute(
                "totalAdmins",
                userService.countByRole(UserRole.ADMIN)
        );
        model.addAttribute(
                "totalRecruiters",
                userService.countByRole(UserRole.RECRUITER)
        );
        model.addAttribute(
                "totalCandidates",
                userService.countByRole(UserRole.CANDIDATE)
        );

        // ===== USER GROWTH: 6 THÁNG =====
        List<Map<String, Object>> growth = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);

            long count = userService.countBetween(
                    month.atDay(1).atStartOfDay(),
                    month.plusMonths(1).atDay(1).atStartOfDay()
            );

            Map<String, Object> row = new HashMap<>();
            row.put("label", month.getMonth().name().substring(0, 3));
            row.put("year", month.getYear());
            row.put("month", month.getMonthValue());
            row.put("count", count);

            growth.add(row);
        }

        model.addAttribute(
                "growthLabels",
                growth.stream().map(r -> r.get("label")).toList()
        );
        model.addAttribute(
                "growthData",
                growth.stream().map(r -> r.get("count")).toList()
        );

        // ===== RECENT ACTIVITY =====
        model.addAttribute(
                "recentActivities",
                activityLogService.recent(5)
        );

        model.addAttribute("activeMenu", "dashboard");

        return "admin/dashboard";
    }
}