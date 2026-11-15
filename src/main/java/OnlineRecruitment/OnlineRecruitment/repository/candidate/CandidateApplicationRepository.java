package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

// =====================================================
// REPOSITORY ĐƠN ỨNG TUYỂN PHÍA ỨNG VIÊN
// Chỉ chứa câu SQL đọc / ghi bảng dbo.applications,
// không chứa logic nghiệp vụ (logic nằm ở CandidateApplicationService).
// =====================================================

@Repository
public class CandidateApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public CandidateApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy danh sách đơn ứng tuyển của ứng viên theo username / email,
    // kèm lịch phỏng vấn SCHEDULED mới nhất của từng đơn (nếu có)
    public List<Map<String, Object>> findApplicationsByLoginName(String loginName) {

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

        return jdbcTemplate.queryForList(sql, loginName, loginName);
    }

    // Lấy danh sách candidate_id ứng với username / email đang đăng nhập
    public List<Integer> findCandidateIdsByLoginName(String loginName) {

        String sql = """
                SELECT ca.id
                FROM dbo.candidates ca
                INNER JOIN dbo.users u ON u.id = ca.user_id
                WHERE u.email = ? OR u.username = ?
                """;

        return jdbcTemplate.queryForList(
                sql,
                Integer.class,
                loginName,
                loginName
        );
    }

    // Đếm số đơn đang chờ xử lý hoặc đã được chấp nhận
    // của ứng viên cho một công việc
    public int countActiveApplications(int candidateId, int jobId) {

        String sql = """
                SELECT COUNT(*)
                FROM dbo.applications
                WHERE candidate_id = ?
                  AND job_id = ?
                  AND UPPER(status) IN ('SUBMITTED', 'ACCEPTED', 'APPROVED')
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                candidateId,
                jobId
        );

        return count == null ? 0 : count;
    }

    // Đếm số đơn bị từ chối của ứng viên cho một công việc
    public int countRejectedApplications(int candidateId, int jobId) {

        String sql = """
                SELECT COUNT(*)
                FROM dbo.applications
                WHERE candidate_id = ?
                  AND job_id = ?
                  AND UPPER(status) = 'REJECTED'
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                candidateId,
                jobId
        );

        return count == null ? 0 : count;
    }

    // Tạo đơn mới (SUBMITTED) bằng cách sao chép công việc, CV,
    // template và snapshot CV từ đơn bị từ chối gần nhất.
    // Trả về số dòng được thêm.
    public int insertFromLatestRejected(int candidateId, int jobId) {

        String sql = """
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

        return jdbcTemplate.update(sql, candidateId, jobId);
    }
}
