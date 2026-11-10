
package OnlineRecruitment.OnlineRecruitment.repository.Recruiter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
@Repository
public class RecruiterWishlistRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecruiterWishlistRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy danh sách candidate đã lưu theo từng job
    public Set<String> findWishlistedCandidateJobs(Long recruiterId) {

        String sql = """
            SELECT candidate_id, job_id
            FROM recruiter_wishlist
            WHERE recruiter_id = ?
            """;

        List<String> savedItems = jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        rs.getLong("candidate_id") + "_" + rs.getLong("job_id"),
                recruiterId
        );

        return new HashSet<>(savedItems);
    }

    // Kiểm tra candidate đã được lưu ở job này chưa
    public boolean exists(Long recruiterId, Long candidateId, Long jobId) {

        String sql = """
            SELECT COUNT(*)
            FROM recruiter_wishlist
            WHERE recruiter_id = ?
              AND candidate_id = ?
              AND job_id = ?
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                recruiterId,
                candidateId,
                jobId
        );

        return count != null && count > 0;
    }

    // Thêm candidate vào Wishlist theo job
    public int addWishlist(
            Long recruiterId,
            Long candidateId,
            Long jobId) {

        String sql = """
            INSERT INTO recruiter_wishlist (
                recruiter_id,
                candidate_id,
                job_id,
                created_at
            )
            SELECT ?, ?, ?, GETDATE()
            WHERE EXISTS (
                SELECT 1
                FROM interview_schedules s
                INNER JOIN applications a
                    ON a.id = s.application_id
                INNER JOIN jobs j
                    ON j.id = a.job_id
                WHERE s.recruiter_id = ?
                  AND s.candidate_id = ?
                  AND j.id = ?
                  AND s.status = 'SCHEDULED'
            )
            AND NOT EXISTS (
                SELECT 1
                FROM recruiter_wishlist
                WHERE recruiter_id = ?
                  AND candidate_id = ?
                  AND job_id = ?
            )
            """;

        return jdbcTemplate.update(
                sql,
                recruiterId,
                candidateId,
                jobId,
                recruiterId,
                candidateId,
                jobId,
                recruiterId,
                candidateId,
                jobId
        );
    }

    // Xóa candidate khỏi Wishlist của đúng job
    public int removeWishlist(
            Long recruiterId,
            Long candidateId,
            Long jobId) {

        String sql = """
            DELETE FROM recruiter_wishlist
            WHERE recruiter_id = ?
              AND candidate_id = ?
              AND job_id = ?
            """;

        return jdbcTemplate.update(
                sql,
                recruiterId,
                candidateId,
                jobId
        );
    }// Lấy thông tin chi tiết ứng viên trong Wishlist
    public List<Map<String, Object>> findWishlistDetails(Long recruiterId) {

        String sql = """
        SELECT
            w.id AS wishlistId,
            w.candidate_id AS candidateId,
            w.job_id AS jobId,
            w.created_at AS createdAt,
            u.username AS candidateName,
            u.email AS email,
            j.title AS jobTitle,
            a.id AS applicationId
        FROM recruiter_wishlist w
        INNER JOIN candidates c
            ON c.id = w.candidate_id
        INNER JOIN users u
            ON u.id = c.user_id
        INNER JOIN jobs j
            ON j.id = w.job_id
        OUTER APPLY (
            SELECT TOP 1 a.id
            FROM applications a
            INNER JOIN interview_schedules s
                ON s.application_id = a.id
            WHERE a.candidate_id = w.candidate_id
              AND a.job_id = w.job_id
              AND s.recruiter_id = w.recruiter_id
              AND s.status = 'SCHEDULED'
            ORDER BY s.created_at DESC
        ) a
        WHERE w.recruiter_id = ?
        ORDER BY w.created_at DESC
        """;

        return jdbcTemplate.queryForList(sql, recruiterId);
    }
}
