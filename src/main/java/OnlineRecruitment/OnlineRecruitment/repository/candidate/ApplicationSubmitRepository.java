package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

// =====================================================
// REPOSITORY NỘP HỒ SƠ ỨNG TUYỂN
// Chứa toàn bộ câu SQL phục vụ luồng nộp đơn:
//   - Tìm user / candidate từ tài khoản đăng nhập
//   - Kiểm tra job, CV, đơn cũ
//   - Đọc các mục của CV để chụp snapshot
//   - Thêm mới / mở lại đơn ứng tuyển
// Không chứa logic nghiệp vụ (logic nằm ở ApplicationSubmitService).
// =====================================================

@Repository
public class ApplicationSubmitRepository {

    private final JdbcTemplate jdbcTemplate;

    public ApplicationSubmitRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // =================================================
    // TÀI KHOẢN / ỨNG VIÊN
    // =================================================

    // Tìm user theo username hoặc email
    public List<Map<String, Object>> findUserByLoginName(String loginName) {
        return jdbcTemplate.queryForList(
                """
                SELECT TOP 1 id
                FROM dbo.users
                WHERE username = ? OR email = ?
                """,
                loginName, loginName
        );
    }

    // Tìm hồ sơ ứng viên gắn với user
    public List<Map<String, Object>> findCandidateByUserId(Long userId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM dbo.candidates WHERE user_id = ?",
                userId
        );
    }

    // =================================================
    // CÔNG VIỆC / CV / ĐƠN CŨ
    // =================================================

    // Tìm công việc đang tuyển: job ACTIVE, công ty ACTIVE, chưa quá hạn
    public List<Map<String, Object>> findOpenJob(Long jobId) {
        return jdbcTemplate.queryForList(
                """
                SELECT j.id, j.company_id, c.recruiter_id
                FROM dbo.jobs j
                JOIN dbo.companies c ON c.id = j.company_id
                WHERE j.id = ?
                  AND j.status = 'ACTIVE' AND c.status = 'ACTIVE'
                  AND (j.deadline IS NULL OR j.deadline >= CONVERT(date, GETDATE()))
                """,
                jobId
        );
    }

    // Tìm CV ACTIVE thuộc đúng ứng viên
    public List<Map<String, Object>> findActiveCv(Long cvId, Long candidateId) {
        return jdbcTemplate.queryForList(
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
                    status AS status,
                    content AS content,
                    avatar_url AS avatarUrl,
                    file_url AS fileUrl,
                    is_uploaded AS isUploaded
                FROM dbo.cv
                WHERE id = ?
                  AND candidate_id = ?
                  AND status = 'ACTIVE'
                """,
                cvId, candidateId
        );
    }

    // Tìm đơn ứng tuyển đã có của ứng viên cho công việc
    public List<Map<String, Object>> findExistingApplications(Long candidateId, Long jobId) {
        return jdbcTemplate.queryForList(
                """
                SELECT id, status
                FROM dbo.applications
                WHERE candidate_id = ?
                  AND job_id = ?
                """,
                candidateId, jobId
        );
    }

    // =================================================
    // CÁC MỤC CỦA CV (DÙNG ĐỂ CHỤP SNAPSHOT)
    // =================================================

    public List<Map<String, Object>> findWorkExperiences(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT company_name, job_title, location,
                       start_date, end_date, is_current, description
                FROM dbo.cv_work_experiences
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findEducations(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT institution, degree, field_of_study,
                       start_date, end_date, grade, description
                FROM dbo.cv_educations
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findSkills(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT s.name AS skillName, cs.proficiency
                FROM dbo.cv_skills cs
                JOIN dbo.skills s ON s.id = cs.skill_id
                WHERE cs.cv_id = ?
                ORDER BY cs.display_order
                """, cvId);
    }

    public List<Map<String, Object>> findProjects(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT name, role, start_date, end_date,
                       project_url, description, technologies
                FROM dbo.cv_projects
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findCertificates(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT name, issuing_org, issue_date, expiry_date,
                       credential_id, credential_url, description
                FROM dbo.cv_certificates
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findAwards(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT name, issuer, award_date, description
                FROM dbo.cv_awards
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findActivities(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT organization, role, start_date, end_date, description
                FROM dbo.cv_activities
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    public List<Map<String, Object>> findReferences(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT full_name, position, organization, email, phone, relationship
                FROM dbo.cv_references
                WHERE cv_id = ?
                ORDER BY display_order
                """, cvId);
    }

    // Lấy file đính kèm ưu tiên của CV (file chính, mới nhất)
    public List<Map<String, Object>> findPrimaryAttachment(Long cvId) {
        return jdbcTemplate.queryForList(
                """
                SELECT TOP 1 storage_path
                FROM dbo.cv_attachments
                WHERE cv_id = ?
                ORDER BY is_primary DESC, uploaded_at DESC
                """,
                cvId
        );
    }

    // =================================================
    // GHI ĐƠN ỨNG TUYỂN
    // =================================================

    // Mở lại đơn đã bị từ chối: chuyển về SUBMITTED, cập nhật ngày nộp.
    // Giữ nguyên dữ liệu CV đã nộp trước đó. Trả về số dòng cập nhật.
    public int reopenRejectedApplication(Long applicationId, Long candidateId, Long jobId) {
        return jdbcTemplate.update(
                """
                UPDATE dbo.applications
                SET status = 'SUBMITTED',
                    applied_at = GETDATE()
                WHERE id = ?
                  AND candidate_id = ?
                  AND job_id = ?
                  AND status = 'REJECTED'
                """,
                applicationId,
                candidateId,
                jobId
        );
    }

    // Tạo đơn ứng tuyển mới kèm snapshot CV
    public int insertApplication(
            Long candidateId,
            Long jobId,
            Long cvId,
            Long templateVersionId,
            String snapshotJson,
            String filePath) {

        return jdbcTemplate.update(
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
    }
}
