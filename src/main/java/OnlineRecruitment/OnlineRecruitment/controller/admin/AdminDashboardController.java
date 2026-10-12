package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.ActivityLog;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.service.ActivityLogService;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.DashboardService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserService userService;
    private final CompanyService companyService;
    private final DashboardService dashboardService;
    private final ActivityLogService activityLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // KPI CARDS
        model.addAttribute("totalUsers",     userService.countAll());
        model.addAttribute("totalCompanies", companyService.countAll());
        model.addAttribute("totalJobs",      dashboardService.totalJobs());

        long companyPending = companyService.countByStatus(CompanyStatus.PENDING);
        long userLocked     = userService.countByStatus(UserStatus.LOCKED);
        long jobPending     = dashboardService.jobsByStatus("PENDING");   // ← đổi ở đây

        model.addAttribute("totalPending",   companyPending + userLocked + jobPending);
        model.addAttribute("companyPending", companyPending);

        // USER ROLE STATS
        model.addAttribute("totalAdmins",     userService.countByRole(UserRole.ADMIN));
        model.addAttribute("totalRecruiters", userService.countByRole(UserRole.RECRUITER));
        model.addAttribute("totalCandidates", userService.countByRole(UserRole.CANDIDATE));

        // USER GROWTH
        List<Map<String, Object>> growth = dashboardService.userGrowthLast6Months();
        model.addAttribute("growthLabels", growth.stream().map(r -> r.get("label")).toList());
        model.addAttribute("growthData",   growth.stream().map(r -> r.get("count")).toList());

        // RECENT ACTIVITY
        List<ActivityLog> recent = activityLogService.recent(5);
        model.addAttribute("recentActivities", recent);

        model.addAttribute("activeMenu", "dashboard");
        return "admin/dashboard";
    }
}