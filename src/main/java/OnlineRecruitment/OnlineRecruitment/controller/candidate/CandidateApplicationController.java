package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class CandidateApplicationController {

    private final JdbcTemplate jdbcTemplate;

    public CandidateApplicationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/candidate/applications")
    public String myApplications(Model model) {

        // Lấy tài khoản đang đăng nhập
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        // Tài khoản đăng nhập (có thể là username hoặc email)
        String loginName = authentication.getName();

        // Lấy danh sách đơn ứng tuyển của tài khoản hiện tại
        String sql = """
                SELECT
                    a.id AS id,
                    j.title AS jobTitle,
                    c.company_name AS companyName,
                    cv.title AS cvTitle,
                    a.status AS status,
                    a.applied_at AS appliedAt
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users u ON u.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE u.email = ? OR u.username = ?
                ORDER BY a.applied_at DESC
                """;

        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(sql, loginName, loginName);

        model.addAttribute("applications", applications);

        return "candidate/my-applications";
    }
}
