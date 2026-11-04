
package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class CandidateApplicationController {

    private final JdbcTemplate jdbcTemplate;

    public CandidateApplicationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy tài khoản đang đăng nhập
    private String getLoginName() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

    // Lấy danh sách đơn ứng tuyển
    @GetMapping("/candidate/applications")
    public String myApplications(Model model) {

        String loginName = getLoginName();

        if (loginName == null) {
            return "redirect:/login";
        }

        String sql = """
    SELECT
        a.id AS id,
        a.job_id AS jobId,
        j.title AS jobTitle,
        COALESCE(c.company_name, N'Chưa có thông tin') AS companyName,
        cv.title AS cvTitle,
        a.status AS status,
        a.applied_at AS appliedAt,

        s.id AS scheduleId,
        s.status AS interviewStatus,
        ia.available_date AS interviewDate,
        ia.start_time AS interviewStartTime,
        ia.end_time AS interviewEndTime,
        s.note AS interviewNote

    FROM dbo.applications a

    INNER JOIN dbo.candidates ca
        ON ca.id = a.candidate_id

    INNER JOIN dbo.users u
        ON u.id = ca.user_id

    INNER JOIN dbo.jobs j
        ON j.id = a.job_id

    LEFT JOIN dbo.companies c
        ON c.id = j.company_id

    LEFT JOIN dbo.cv cv
        ON cv.id = a.cv_id

    OUTER APPLY (
        SELECT TOP 1
            sch.id,
            sch.status,
            sch.note,
            sch.availability_id
        FROM dbo.interview_schedules sch
        WHERE sch.application_id = a.id
          AND sch.status = 'SCHEDULED'
        ORDER BY sch.created_at DESC, sch.id DESC
    ) s

    LEFT JOIN dbo.interview_availability ia
        ON ia.id = s.availability_id

    WHERE (u.email = ? OR u.username = ?)

    ORDER BY a.applied_at DESC, a.id DESC
    """;

        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(sql, loginName, loginName);

        model.addAttribute("applications", applications);

        return "candidate/my-applications";
    }

    // NỘP LẠI ĐƠN ỨNG TUYỂN
    @PostMapping("/candidate/apply-again/{jobId}")
    @Transactional
    public String applyAgain(
            @PathVariable("jobId") int jobId,
            RedirectAttributes redirectAttributes) {

        String loginName = getLoginName();

        if (loginName == null) {
            return "redirect:/login";
        }

        // 1. Xác định đúng ứng viên đang đăng nhập
        String candidateSql = """
                SELECT ca.id
                FROM dbo.candidates ca
                INNER JOIN dbo.users u ON u.id = ca.user_id
                WHERE u.email = ? OR u.username = ?
                """;

        List<Integer> candidateIds = jdbcTemplate.queryForList(
                candidateSql,
                Integer.class,
                loginName,
                loginName
        );

        if (candidateIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Không tìm thấy hồ sơ ứng viên."
            );
            return "redirect:/candidate/applications";
        }

        int candidateId = candidateIds.get(0);

        // 2. Kiểm tra đã có đơn đang chờ hoặc đã được chấp nhận chưa
        String activeSql = """
                SELECT COUNT(*)
                FROM dbo.applications
                WHERE candidate_id = ?
                  AND job_id = ?
                  AND UPPER(status) IN ('SUBMITTED', 'ACCEPTED', 'APPROVED')
                """;

        Integer activeCount = jdbcTemplate.queryForObject(
                activeSql,
                Integer.class,
                candidateId,
                jobId
        );

        if (activeCount != null && activeCount > 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bạn đã có đơn đang chờ xử lý hoặc đã được chấp nhận."
            );
            return "redirect:/candidate/applications";
        }

        // 3. Chỉ cho phép nộp lại nếu có đơn cũ bị từ chối
        String rejectedSql = """
                SELECT COUNT(*)
                FROM dbo.applications
                WHERE candidate_id = ?
                  AND job_id = ?
                  AND UPPER(status) = 'REJECTED'
                """;

        Integer rejectedCount = jdbcTemplate.queryForObject(
                rejectedSql,
                Integer.class,
                candidateId,
                jobId
        );

        if (rejectedCount == null || rejectedCount == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Bạn không có đơn bị từ chối cho công việc này."
            );
            return "redirect:/candidate/applications";
        }

        // 4. Tạo đơn mới, giữ nguyên công việc, công ty và CV
        // từ đơn bị từ chối gần nhất.
        String insertSql = """
                INSERT INTO dbo.applications (
                    candidate_id,
                    job_id,
                    cv_id,
                    cv_template_version_id,
                    cv_snapshot_json,
                    cv_file_path_snapshot,
                    status,
                    rejection_reason,
                    applied_at,
                    updated_at
                )
                SELECT TOP (1)
                    a.candidate_id,
                    a.job_id,
                    a.cv_id,
                    a.cv_template_version_id,
                    a.cv_snapshot_json,
                    a.cv_file_path_snapshot,
                    'SUBMITTED',
                    NULL,
                    GETDATE(),
                    GETDATE()
                FROM dbo.applications a
                INNER JOIN dbo.jobs j ON j.id = a.job_id
                WHERE a.candidate_id = ?
                  AND a.job_id = ?
                  AND UPPER(a.status) = 'REJECTED'
                ORDER BY a.applied_at DESC, a.id DESC
                """;

        int rows = jdbcTemplate.update(
                insertSql,
                candidateId,
                jobId
        );

        if (rows > 0) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nộp lại đơn xin việc thành công!"
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể nộp lại đơn. Vui lòng thử lại."
            );
        }

        return "redirect:/candidate/applications";
    }
}
