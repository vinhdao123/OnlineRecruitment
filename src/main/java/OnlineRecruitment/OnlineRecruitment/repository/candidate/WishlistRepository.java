
package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WishlistRepository {

    private final JdbcTemplate jdbcTemplate;

    public WishlistRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Kiểm tra công việc đã được yêu thích chưa
    public boolean existsByCandidateIdAndJobId(
            Long candidateId, Long jobId) {

        String sql = """
                SELECT COUNT(*)
                FROM wishlist
                WHERE candidate_id = ?
                  AND job_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                candidateId,
                jobId
        );

        return count != null && count > 0;
    }

    // Thêm công việc vào Wishlist
    public void add(Long candidateId, Long jobId) {

        String sql = """
                INSERT INTO wishlist (candidate_id, job_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(sql, candidateId, jobId);
    }

    // Xóa công việc khỏi Wishlist
    public void remove(Long candidateId, Long jobId) {

        String sql = """
                DELETE FROM wishlist
                WHERE candidate_id = ?
                  AND job_id = ?
                """;

        jdbcTemplate.update(sql, candidateId, jobId);
    }

    // Lấy danh sách Job đã yêu thích của Candidate
    public List<Long> findJobIdsByCandidateId(Long candidateId) {

        String sql = """
                SELECT job_id
                FROM wishlist
                WHERE candidate_id = ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("job_id"),
                candidateId
        );
    }
}
