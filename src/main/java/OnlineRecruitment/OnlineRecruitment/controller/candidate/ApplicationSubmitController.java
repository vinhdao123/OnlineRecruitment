package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ApplicationSubmitController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ApplicationSubmitController(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/applications/submit")
    @Transactional
    public String submitApplication(
            @RequestParam Long jobId,
            @RequestParam Long cvId,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) Boolean confirmInformation,
            RedirectAttributes redirectAttributes) {

        String redirectUrl = "redirect:/jobs/" + jobId + "/apply";

        try {
            // 1. Kiểm tra người dùng đang đăng nhập
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getName())) {
                return "redirect:/login";
            }

            String loginName = authentication.getName();

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    """
                    SELECT TOP 1 id
                    FROM dbo.users
                    WHERE username = ? OR email = ?
                    """,
                    loginName, loginName
            );

            if (users.isEmpty()) {
                throw new IllegalArgumentException(
                        "Không tìm thấy tài khoản đang đăng nhập.");
            }

            Long userId = ((Number) users.get(0).get("id")).longValue();

            // 2. Lấy candidate ID từ tài khoản hiện tại
            List<Map<String, Object>> candidates = jdbcTemplate.queryForList(
                    "SELECT id FROM dbo.candidates WHERE user_id = ?",
                    userId
            );

            if (candidates.isEmpty()) {
                throw new IllegalArgumentException(
                        "Tài khoản chưa có hồ sơ ứng viên.");
            }

            Long candidateId =
                    ((Number) candidates.get(0).get("id")).longValue();

            // 3. Kiểm tra công việc và công ty sở hữu công việc
            List<Map<String, Object>> jobs = jdbcTemplate.queryForList(
                    """
                    SELECT j.id, j.company_id, c.recruiter_id
                    FROM dbo.jobs j
                    JOIN dbo.companies c ON c.id = j.company_id
                    WHERE j.id = ?
                    """,
                    jobId
            );

            if (jobs.isEmpty()) {
                throw new IllegalArgumentException(
                        "Không tìm thấy công việc hoặc công ty tuyển dụng.");
            }

            // 4. Chỉ chấp nhận CV ACTIVE thuộc đúng ứng viên
            List<Map<String, Object>> cvs = jdbcTemplate.queryForList(
                    """
                    SELECT
                        id AS id,
                        candidate_id AS candidateId,
                        template_version_id AS templateVersionId,
                        title AS title,
                        professional_title AS professionalTitle,
                        summary AS summary,
                        accent_color AS accentColor,
                        is_default AS isDefault,
                        status AS status
                    FROM dbo.cv
                    WHERE id = ?
                      AND candidate_id = ?
                      AND status = 'ACTIVE'
                    """,
                    cvId, candidateId
            );

            if (cvs.isEmpty()) {
                throw new IllegalArgumentException(
                        "CV không tồn tại hoặc không thuộc tài khoản của bạn.");
            }

            Map<String, Object> cv = cvs.get(0);

            Long templateVersionId =
                    ((Number) cv.get("templateVersionId")).longValue();

            // 5. Không cho ứng tuyển trùng cùng một công việc
            Integer existing = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM dbo.applications
                    WHERE candidate_id = ? AND job_id = ?
                    """,
                    Integer.class, candidateId, jobId
            );

            if (existing != null && existing > 0) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Bạn đã ứng tuyển công việc này rồi.");
                return redirectUrl;
            }

            // 6. Tạo bản chụp dữ liệu CV tại thời điểm ứng tuyển
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("cv", cv);

            snapshot.put("workExperiences", jdbcTemplate.queryForList(
                    """
                    SELECT company_name, job_title, location,
                           start_date, end_date, is_current, description
                    FROM dbo.cv_work_experiences
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("educations", jdbcTemplate.queryForList(
                    """
                    SELECT institution, degree, field_of_study,
                           start_date, end_date, grade, description
                    FROM dbo.cv_educations
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("skills", jdbcTemplate.queryForList(
                    """
                    SELECT s.name AS skillName, cs.proficiency
                    FROM dbo.cv_skills cs
                    JOIN dbo.skills s ON s.id = cs.skill_id
                    WHERE cs.cv_id = ?
                    ORDER BY cs.display_order
                    """, cvId));

            snapshot.put("projects", jdbcTemplate.queryForList(
                    """
                    SELECT name, role, start_date, end_date,
                           project_url, description, technologies
                    FROM dbo.cv_projects
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("certificates", jdbcTemplate.queryForList(
                    """
                    SELECT name, issuing_org, issue_date, expiry_date,
                           credential_id, credential_url
                    FROM dbo.cv_certificates
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("awards", jdbcTemplate.queryForList(
                    """
                    SELECT name, issuer, award_date, description
                    FROM dbo.cv_awards
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("activities", jdbcTemplate.queryForList(
                    """
                    SELECT organization, role, start_date, end_date, description
                    FROM dbo.cv_activities
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            snapshot.put("references", jdbcTemplate.queryForList(
                    """
                    SELECT full_name, position, organization, email, phone
                    FROM dbo.cv_references
                    WHERE cv_id = ?
                    ORDER BY display_order
                    """, cvId));

            String snapshotJson =
                    objectMapper.writeValueAsString(snapshot);

            // 7. Lấy đường dẫn file đính kèm nếu có
            List<Map<String, Object>> attachments =
                    jdbcTemplate.queryForList(
                            """
                            SELECT TOP 1 storage_path
                            FROM dbo.cv_attachments
                            WHERE cv_id = ?
                            ORDER BY is_primary DESC, uploaded_at DESC
                            """,
                            cvId
                    );

            String filePath = attachments.isEmpty()
                    ? null
                    : (String) attachments.get(0).get("storage_path");

            // 8. Lưu đơn ứng tuyển gắn với đúng job, candidate và CV
            jdbcTemplate.update(
                    """
                    INSERT INTO dbo.applications
                    (
                        candidate_id,
                        job_id,
                        cv_id,
                        cv_template_version_id,
                        cv_snapshot_json,
                        cv_file_path_snapshot,
                        status
                    )
                    VALUES (?, ?, ?, ?, ?, ?, 'SUBMITTED')
                    """,
                    candidateId,
                    jobId,
                    cvId,
                    templateVersionId,
                    snapshotJson,
                    filePath
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Gửi hồ sơ ứng tuyển thành công!");

            return redirectUrl;

        } catch (JacksonException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể tạo bản lưu CV. Vui lòng thử lại.");
            return redirectUrl;

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Gửi hồ sơ thất bại: " + e.getMessage());
            return redirectUrl;
        }
    }
}
