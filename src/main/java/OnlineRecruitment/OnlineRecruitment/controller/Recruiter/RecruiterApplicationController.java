package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/recruiter/applications")
public class RecruiterApplicationController {

    private final JdbcTemplate jdbcTemplate;

    public RecruiterApplicationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =========================================
    // DANH SÁCH ĐƠN ỨNG TUYỂN
    // =========================================sql.append(" ORDER BY a.applied_at DESC");
    @GetMapping
    public String applications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "SUBMITTED") String status,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Model model) {


        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            model.addAttribute("applications", List.of());
            model.addAttribute("jobs", List.of());
            addFilters(model, keyword, status, jobId, sort);
            return "recruiter/applications";
        }

        Object recruiterId = users.get(0).get("id");

        // Chỉ lấy công việc thuộc công ty của recruiter đang đăng nhập.
        List<Map<String, Object>> jobs = jdbcTemplate.queryForList("""
                SELECT j.id, j.title
                FROM dbo.jobs j
                JOIN dbo.companies c ON c.id = j.company_id
                WHERE c.recruiter_id = ?
                ORDER BY j.title
                """, recruiterId);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    a.id AS id,
                    a.candidate_id AS candidateId,
                    a.job_id AS jobId,
                    a.cv_id AS cvId,
                    a.cv_snapshot_json AS cvSnapshotJson,
                    u.full_name AS candidateName,
                    u.email AS email,
                    u.phone AS phone,
                    j.title AS jobTitle,
                    c.company_name AS companyName,
                    j.location AS location,
                    a.applied_at AS appliedAt,
                    a.status AS status,
                    cv.title AS cvTitle,
                    cv.professional_title AS cvProfessionalTitle,
                    cv.summary AS cvSummary
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users u ON u.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE c.recruiter_id = ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(recruiterId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }

        if (jobId != null) {
            sql.append(" AND j.id = ?");
            params.add(jobId);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append("""
                     AND (
                         u.full_name LIKE ?
                         OR u.email LIKE ?
                         OR j.title LIKE ?
                     )
                    """);

            String search = "%" + keyword.trim() + "%";
            params.add(search);
            params.add(search);
            params.add(search);
        }


        if ("oldest".equals(sort)) {
            sql.append(" ORDER BY a.applied_at ASC");
        } else {
            sql.append(" ORDER BY a.applied_at DESC");
        }


        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(sql.toString(), params.toArray());

        model.addAttribute("applications", applications);
        model.addAttribute("jobs", jobs);
        addFilters(model, keyword, status, jobId, sort);

        return "recruiter/applications";
    }

    // =========================================
    // XEM CV THEO ĐƠN ỨNG TUYỂN
    // =========================================
    @GetMapping("/{applicationId}/cv")
    public String viewApplicationCv(
            @PathVariable Long applicationId,
            Model model) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return "redirect:/login";
        }

        String username = auth.getName();

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tài khoản nhà tuyển dụng."
            );
        }

        Object recruiterId = users.get(0).get("id");

        // Chỉ xem đơn thuộc công ty của recruiter đang đăng nhập.
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                    a.id AS applicationId,
                    a.candidate_id AS candidateId,
                    a.job_id AS jobId,
                    a.cv_id AS cvId,
                    a.cv_snapshot_json AS cvSnapshotJson,
                    u.full_name AS candidateName,
                    u.email AS email,
                    u.phone AS phone,
                    j.title AS jobTitle,
                    c.company_name AS companyName,
                    cv.title AS cvTitle,
                    cv.professional_title AS professionalTitle,
                    cv.summary AS summary
                FROM dbo.applications a
                JOIN dbo.candidates ca ON ca.id = a.candidate_id
                JOIN dbo.users u ON u.id = ca.user_id
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                LEFT JOIN dbo.cv cv ON cv.id = a.cv_id
                WHERE a.id = ?
                  AND c.recruiter_id = ?
                """, applicationId, recruiterId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn ứng tuyển hoặc bạn không có quyền xem."
            );
        }

        // Sao chép dữ liệu từ kết quả SQL sang Map riêng.
        Map<String, Object> applicationData =
                new HashMap<>(rows.get(0));

        // Dùng tên applicationData để tránh nhầm với các biến
        // ngầm định của môi trường Thymeleaf.
        model.addAttribute("applicationData", applicationData);

        // Debug để kiểm tra dữ liệu được chuyển sang giao diện.
        System.out.println("========== DEBUG CV ==========");
        System.out.println("Application ID: "
                + applicationData.get("applicationId"));
        System.out.println("Candidate ID: "
                + applicationData.get("candidateId"));
        System.out.println("Candidate: "
                + applicationData.get("candidateName"));
        System.out.println("Email: "
                + applicationData.get("email"));
        System.out.println("Phone: "
                + applicationData.get("phone"));
        System.out.println("Job: "
                + applicationData.get("jobTitle"));
        System.out.println("Company: "
                + applicationData.get("companyName"));
        System.out.println("CV ID: "
                + applicationData.get("cvId"));
        System.out.println("CV title: "
                + applicationData.get("cvTitle"));
        System.out.println("==============================");

        return "recruiter/cv-preview";
    }

    // =========================================
    // DỮ LIỆU BỘ LỌC
    // =========================================

    private void addFilters(
            Model model,
            String keyword,
            String status,
            Long jobId,
            String sort) {

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("jobId", jobId);
        model.addAttribute("sort", sort);
    }




    // Duyệt đơn ứng tuyển
    @PostMapping("/{applicationId}/approve")
    public String approveApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            return "redirect:/login";
        }

        Object recruiterId = users.get(0).get("id");

        int updated = jdbcTemplate.update("""
        UPDATE a
        SET a.status = 'ACCEPTED'
        FROM dbo.applications a
        JOIN dbo.jobs j ON j.id = a.job_id
        JOIN dbo.companies c ON c.id = j.company_id
        WHERE a.id = ?
          AND c.recruiter_id = ?
          AND a.status = 'SUBMITTED'
        """, applicationId, recruiterId);

        if (updated > 0) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã duyệt đơn ứng tuyển thành công.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác.");
        }

        return "redirect:/recruiter/applications?status=ACCEPTED";
    }

    // Từ chối đơn ứng tuyển
    @PostMapping("/{applicationId}/reject")
    public String rejectApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        List<Map<String, Object>> users = jdbcTemplate.queryForList("""
                SELECT id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """, username, username);

        if (users.isEmpty()) {
            return "redirect:/login";
        }

        Object recruiterId = users.get(0).get("id");

        int updated = jdbcTemplate.update("""
                UPDATE a
                SET a.status = 'REJECTED'
                FROM dbo.applications a
                JOIN dbo.jobs j ON j.id = a.job_id
                JOIN dbo.companies c ON c.id = j.company_id
                WHERE a.id = ?
                  AND c.recruiter_id = ?
                  AND a.status = 'SUBMITTED'
                """, applicationId, recruiterId);

        if (updated > 0) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã từ chối đơn ứng tuyển.");
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy đơn chờ duyệt hoặc bạn không có quyền thao tác.");
        }

        return "redirect:/recruiter/applications?status=REJECTED";
    }

    // Lấy email hoặc username của tài khoản đang đăng nhập
    private String getCurrentUsername() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        return auth.getName();
    }

}