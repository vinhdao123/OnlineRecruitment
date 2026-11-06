package OnlineRecruitment.OnlineRecruitment.controller;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
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

    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/candidate/dashboard")
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
    public String recruiterDashboard() {
        return "recruiter/dashboard";
    }

    @RequestMapping("/403")
    public String forbidden() {
        return "403";
    }
}
