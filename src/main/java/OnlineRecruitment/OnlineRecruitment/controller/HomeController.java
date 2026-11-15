package OnlineRecruitment.OnlineRecruitment.controller;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.JobCategoryRepository;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@Controller
public class HomeController {

    @Autowired(required = false)
    private JobRepository jobRepository;

    @Autowired(required = false)
    private CompanyRepository companyRepository;

    @Autowired(required = false)
    private JobCategoryRepository jobCategoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        if (jobRepository != null) {
            try {
                List<Job> activeJobs = jobRepository.findActiveJobs();
                if (activeJobs != null && !activeJobs.isEmpty()) {
                    model.addAttribute("featuredJobs", activeJobs.stream().limit(6).toList());
                    model.addAttribute("totalActiveJobs", activeJobs.size());
                }
            } catch (Exception ignored) {}
        }
        if (companyRepository != null) {
            try {
                List<Company> companies = companyRepository.findAll();
                if (companies != null && !companies.isEmpty()) {
                    List<Company> activeCompanies = companies.stream()
                            .filter(c -> c.getStatus() == CompanyStatus.ACTIVE)
                            .limit(4)
                            .toList();
                    model.addAttribute("topCompanies", activeCompanies);
                }
            } catch (Exception ignored) {}
        }
        if (jobCategoryRepository != null) {
            try {
                List<JobCategory> categories = jobCategoryRepository.findAll().stream()
                        .filter(c -> c.getStatus() != null && "ACTIVE".equalsIgnoreCase(c.getStatus().trim()))
                        .limit(8)
                        .toList();
                model.addAttribute("categories", categories);
            } catch (Exception ignored) {}
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register() {
        return "redirect:/register/candidate";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/companies")
    public String companies() {
        return "redirect:/jobs";
    }

//    @GetMapping("/candidate/dashboard")
//    public String candidateDashboard() {
//        return "candidate/dashboard";
//    }

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserPackageRepository userPackageRepository;

    public HomeController(JdbcTemplate jdbcTemplate, UserRepository userRepository, UserPackageRepository userPackageRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.userPackageRepository = userPackageRepository;
    }

    @GetMapping("/candidate/dashboard/overview")
    public String candidateDashboard(Model model) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        // Tổng số đơn ứng tuyển của ứng viên đang đăng nhập
        String totalSql = """
        SELECT COUNT(*)
        FROM dbo.applications a
        JOIN dbo.candidates ca ON ca.id = a.candidate_id
        JOIN dbo.users u ON u.id = ca.user_id
        WHERE u.username = ?
        """;

        Integer totalApplications =
                jdbcTemplate.queryForObject(totalSql, Integer.class, username);

        // Số đơn đang chờ duyệt
        String pendingSql = """
        SELECT COUNT(*)
        FROM dbo.applications a
        JOIN dbo.candidates ca ON ca.id = a.candidate_id
        JOIN dbo.users u ON u.id = ca.user_id
        WHERE u.username = ?
          AND a.status = 'SUBMITTED'
        """;

        Integer pendingApplications =
                jdbcTemplate.queryForObject(pendingSql, Integer.class, username);

        model.addAttribute("totalApplications",
                totalApplications != null ? totalApplications : 0);

        model.addAttribute("pendingApplications",
                pendingApplications != null ? pendingApplications : 0);

        return "candidate/dashboard";
    }

    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String principalName = authentication.getName();
            if (userRepository != null) {
                User user = userRepository.findByEmail(principalName)
                        .or(() -> userRepository.findByUsername(principalName))
                        .orElse(null);
                if (user != null) {
                    model.addAttribute("currentUser", user);
                    if (userPackageRepository != null) {
                        UserPackage userPackage = userPackageRepository
                                .findFirstByUserAndStatusOrderByEndDateDesc(user, "ACTIVE")
                                .orElse(null);
                        if (userPackage != null && userPackage.getEndDate() != null && userPackage.getEndDate().isBefore(java.time.LocalDateTime.now())) {
                            userPackage.setStatus("EXPIRED");
                            userPackageRepository.save(userPackage);
                            userPackage = null;
                        }
                        model.addAttribute("currentUserPackage", userPackage);
                    }

                    if (jdbcTemplate != null) {
                        try {
                            Integer totalJobs = jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM dbo.jobs WHERE recruiter_id = ?",
                                    Integer.class, user.getId());
                            model.addAttribute("totalJobs", totalJobs != null ? totalJobs : 0);

                            Integer activeJobs = jdbcTemplate.queryForObject(
                                    "SELECT COUNT(*) FROM dbo.jobs WHERE recruiter_id = ? AND status = 'ACTIVE'",
                                    Integer.class, user.getId());
                            model.addAttribute("activeJobs", activeJobs != null ? activeJobs : 0);

                            Integer pendingApplications = jdbcTemplate.queryForObject("""
                                    SELECT COUNT(*)
                                    FROM dbo.applications a
                                    JOIN dbo.jobs j ON j.id = a.job_id
                                    WHERE j.recruiter_id = ? AND a.status = 'SUBMITTED'
                                    """, Integer.class, user.getId());
                            model.addAttribute("pendingApplications", pendingApplications != null ? pendingApplications : 0);

                            Integer totalApplications = jdbcTemplate.queryForObject("""
                                    SELECT COUNT(*)
                                    FROM dbo.applications a
                                    JOIN dbo.jobs j ON j.id = a.job_id
                                    WHERE j.recruiter_id = ?
                                    """, Integer.class, user.getId());
                            model.addAttribute("totalApplications", totalApplications != null ? totalApplications : 0);

                            Integer acceptedApplications = jdbcTemplate.queryForObject("""
                                    SELECT COUNT(*)
                                    FROM dbo.applications a
                                    JOIN dbo.jobs j ON j.id = a.job_id
                                    WHERE j.recruiter_id = ? AND a.status = 'ACCEPTED'
                                    """, Integer.class, user.getId());
                            model.addAttribute("acceptedApplications", acceptedApplications != null ? acceptedApplications : 0);

                            java.util.List<java.util.Map<String, Object>> recentJobs = jdbcTemplate.queryForList("""
                                    SELECT TOP 5 j.id, j.title, j.status, j.location, j.quantity, j.created_at, j.salary_min, j.salary_max, j.salary_currency,
                                           (SELECT COUNT(*) FROM dbo.applications a WHERE a.job_id = j.id) AS applicationCount
                                    FROM dbo.jobs j
                                    WHERE j.recruiter_id = ?
                                    ORDER BY j.created_at DESC
                                    """, user.getId());
                            model.addAttribute("recentJobs", recentJobs != null ? recentJobs : java.util.List.of());

                            java.util.List<java.util.Map<String, Object>> recentApplications = jdbcTemplate.queryForList("""
                                    SELECT TOP 5 a.id, a.status, a.applied_at, u.full_name AS candidateName, u.email AS candidateEmail, u.avatar_url AS candidateAvatar, j.title AS jobTitle, a.job_id AS jobId
                                    FROM dbo.applications a
                                    JOIN dbo.candidates ca ON ca.id = a.candidate_id
                                    JOIN dbo.users u ON u.id = ca.user_id
                                    JOIN dbo.jobs j ON j.id = a.job_id
                                    WHERE j.recruiter_id = ?
                                    ORDER BY a.applied_at DESC
                                    """, user.getId());
                            model.addAttribute("recentApplications", recentApplications != null ? recentApplications : java.util.List.of());
                        } catch (Exception ignored) {
                            model.addAttribute("totalJobs", 0);
                            model.addAttribute("activeJobs", 0);
                            model.addAttribute("pendingApplications", 0);
                            model.addAttribute("totalApplications", 0);
                            model.addAttribute("acceptedApplications", 0);
                            model.addAttribute("recentJobs", java.util.List.of());
                            model.addAttribute("recentApplications", java.util.List.of());
                        }
                    }
                }
            }
        }
        return "recruiter/dashboard";
    }

    @RequestMapping("/403")
    public String forbidden() {
        return "403";
    }
}
