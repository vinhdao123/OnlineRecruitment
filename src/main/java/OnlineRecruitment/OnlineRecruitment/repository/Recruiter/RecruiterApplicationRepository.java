package OnlineRecruitment.OnlineRecruitment.repository.Recruiter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class RecruiterApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecruiterApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findRecruiterIdByLoginName(String username) {

        String sql = """
            SELECT id
            FROM dbo.users
            WHERE username = ? OR email = ?
            """;

        List<Long> users = jdbcTemplate.queryForList(
                sql,
                Long.class,
                username,
                username
        );

        return users.isEmpty() ? null : users.get(0);
    }

    public List<Map<String, Object>> findJobsByRecruiterId(Long recruiterId) {

        String sql = """
            SELECT j.id, j.title
            FROM dbo.jobs j
            JOIN dbo.companies c ON c.id = j.company_id
            WHERE c.recruiter_id = ?
            ORDER BY j.title
            """;

        return jdbcTemplate.queryForList(sql, recruiterId);
    }

    public List<Map<String, Object>> findApplications(
            Long recruiterId,
            String status,
            Long jobId,
            String keyword,
            String sort) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                a.id AS id,
                a.candidate_id AS candidateId,
                a.job_id AS jobId,
                a.cv_id AS cvId,
                a.cv_template_version_id AS cvTemplateVersionId,
                a.cv_snapshot_json AS cvSnapshotJson,
                a.cv_file_path_snapshot AS cvFilePathSnapshot,
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

        return jdbcTemplate.queryForList(
                sql.toString(),
                params.toArray()
        );
    }

    public List<Map<String, Object>> findApplicationCv(
            Long applicationId,
            Long recruiterId) {

        String sql = """
            SELECT
                a.id AS applicationId,
                a.candidate_id AS candidateId,
                a.job_id AS jobId,
                a.cv_id AS cvId,
                a.cv_template_version_id AS cvTemplateVersionId,
                a.cv_snapshot_json AS cvSnapshotJson,
                u.full_name AS candidateName,
                a.cv_file_path_snapshot AS cvFilePathSnapshot,
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
            """;

        return jdbcTemplate.queryForList(
                sql,
                applicationId,
                recruiterId
        );
    }

    public int approveApplication(Long applicationId, Long recruiterId) {

        String sql = """
            UPDATE a
            SET a.status = 'ACCEPTED'
            FROM dbo.applications a
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies c ON c.id = j.company_id
            WHERE a.id = ?
              AND c.recruiter_id = ?
              AND a.status = 'SUBMITTED'
            """;

        return jdbcTemplate.update(sql, applicationId, recruiterId);
    }
    public List<Map<String, Object>> findApprovalEmailData(
            Long applicationId,
            Long recruiterId) {

        String sql = """
            
                SELECT
                a.id AS applicationId,
                a.candidate_id AS candidateId,
                a.job_id AS jobId,
                a.cv_id AS cvId,
                a.cv_template_version_id AS cvTemplateVersionId,
                a.cv_snapshot_json AS cvSnapshotJson,
                a.cv_file_path_snapshot AS cvFilePathSnapshot,
                company.id AS companyId,
                j.deadline AS applicationDeadline,
                candidate.full_name AS candidateName,
                candidate.email AS candidateEmail,
                recruiter.email AS recruiterEmail,
                company.company_name AS companyName,
                j.title AS jobTitle
            FROM dbo.applications a
            JOIN dbo.candidates ca ON ca.id = a.candidate_id
            JOIN dbo.users candidate ON candidate.id = ca.user_id
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies company ON company.id = j.company_id
            JOIN dbo.users recruiter ON recruiter.id = company.recruiter_id
            WHERE a.id = ?
              AND company.recruiter_id = ?
            """;

        return jdbcTemplate.queryForList(
                sql,
                applicationId,
                recruiterId
        );
    }
    public int rejectApplication(Long applicationId, Long recruiterId) {

        String sql = """
            UPDATE a
            SET a.status = 'REJECTED'
            FROM dbo.applications a
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies c ON c.id = j.company_id
            WHERE a.id = ?
              AND c.recruiter_id = ?
              AND a.status = 'SUBMITTED'
            """;

        return jdbcTemplate.update(sql, applicationId, recruiterId);
    }
    public List<Map<String, Object>> findRejectionEmailData(
            Long applicationId,
            Long recruiterId) {

        String sql = """
            
                SELECT
                                       a.id AS applicationId,
                                       a.candidate_id AS candidateId,
                                       a.job_id AS jobId,
                                       a.cv_id AS cvId,
                                       a.cv_template_version_id AS cvTemplateVersionId,
                                       a.cv_snapshot_json AS cvSnapshotJson,
                                       a.cv_file_path_snapshot AS cvFilePathSnapshot,
                                       company.id AS companyId,
                                       j.deadline AS applicationDeadline,
                                       candidate.full_name AS candidateName,
                                       candidate.email AS candidateEmail,
                                       company.company_name AS companyName,
                                       j.title AS jobTitle
            FROM dbo.applications a
            JOIN dbo.candidates ca ON ca.id = a.candidate_id
            JOIN dbo.users candidate ON candidate.id = ca.user_id
            JOIN dbo.jobs j ON j.id = a.job_id
            JOIN dbo.companies company ON company.id = j.company_id
            WHERE a.id = ?
              AND company.recruiter_id = ?
            """;

        return jdbcTemplate.queryForList(
                sql,
                applicationId,
                recruiterId
        );
    }





}
