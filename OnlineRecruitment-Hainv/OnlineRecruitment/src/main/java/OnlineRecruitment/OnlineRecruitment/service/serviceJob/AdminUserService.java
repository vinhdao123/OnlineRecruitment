package OnlineRecruitment.OnlineRecruitment.service.serviceJob;

import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;

import java.time.LocalDateTime;

import OnlineRecruitment.OnlineRecruitment.dto.AdminUserViewDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class AdminUserService {

    private final JdbcTemplate jdbcTemplate;

    public AdminUserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AdminUserViewDto> findUsers(String keyword) {

        String sql = """
                SELECT
                    id,
                    username,
                    email,
                    full_name,
                    phone,
                    role,
                    status,
                    created_at
                FROM users
                WHERE
                    username LIKE ?
                    OR email LIKE ?
                    OR full_name LIKE ?
                ORDER BY created_at DESC
                """;

        String search = "%" + (keyword == null ? "" : keyword.trim()) + "%";

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {

                    Timestamp createdAt = rs.getTimestamp("created_at");

                    return new AdminUserViewDto(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("full_name"),
                            rs.getString("phone"),
                            rs.getString("role"),
                            rs.getString("status"),
                            createdAt != null
                                    ? createdAt.toLocalDateTime()
                                    : null
                    );
                },
                search,
                search,
                search
        );
    }

    public void deleteUser(Long userId) {

        String role = jdbcTemplate.query(
                "SELECT role FROM users WHERE id = ?",
                rs -> rs.next() ? rs.getString("role") : null,
                userId
        );

        if (role == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy tài khoản."
            );
        }

        // Không cho xóa ADMIN
        if ("ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException(
                    "Không thể xóa tài khoản ADMIN."
            );
        }

        try {

            // =====================================================
            // 1. Xóa token
            // =====================================================
            jdbcTemplate.update(
                    "DELETE FROM tokens WHERE user_id = ?",
                    userId
            );

            // =====================================================
            // 2. Xóa notification
            // =====================================================
            jdbcTemplate.update(
                    "DELETE FROM notifications WHERE user_id = ?",
                    userId
            );

            // =====================================================
            // 3. Xóa application email của recruiter
            // =====================================================
            jdbcTemplate.update(
                    "DELETE FROM application_emails WHERE recruiter_id = ?",
                    userId
            );

            // =====================================================
            // 4. Xóa email schedule
            // =====================================================
            jdbcTemplate.update(
                    "DELETE FROM email_schedules WHERE created_by = ?",
                    userId
            );

            // =====================================================
            // 5. Xóa email template
            // =====================================================
            jdbcTemplate.update(
                    "DELETE FROM email_templates WHERE created_by = ?",
                    userId
            );

            // =====================================================
            // 6. RECRUITER
            // =====================================================
            if ("RECRUITER".equalsIgnoreCase(role)) {

                // Xóa job_skills trước khi xóa jobs
                jdbcTemplate.update("""
                        DELETE FROM job_skills
                        WHERE job_id IN (
                            SELECT id
                            FROM jobs
                            WHERE recruiter_id = ?
                        )
                        """, userId);

                // Xóa jobs
                jdbcTemplate.update(
                        "DELETE FROM jobs WHERE recruiter_id = ?",
                        userId
                );

                // Xóa companies
                jdbcTemplate.update(
                        "DELETE FROM companies WHERE recruiter_id = ?",
                        userId
                );
            }

            // =====================================================
            // 7. CANDIDATE
            // =====================================================
            if ("CANDIDATE".equalsIgnoreCase(role)) {

                // Lấy candidate_id
                Long candidateId = jdbcTemplate.query(
                        "SELECT id FROM candidates WHERE user_id = ?",
                        rs -> rs.next() ? rs.getLong("id") : null,
                        userId
                );

                if (candidateId != null) {

                    // ---------------------------------------------
                    // 7.1 Xóa application_emails
                    // FK: application_emails.candidate_id
                    // -> candidates.id
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM application_emails WHERE candidate_id = ?",
                            candidateId
                    );

                    // ---------------------------------------------
                    // 7.2 Xóa applications
                    // FK: applications.candidate_id
                    // -> candidates.id
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM applications WHERE candidate_id = ?",
                            candidateId
                    );

                    // ---------------------------------------------
                    // 7.3 Xóa company_reviews
                    // FK: company_reviews.candidate_id
                    // -> candidates.id
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM company_reviews WHERE candidate_id = ?",
                            candidateId
                    );

                    // ---------------------------------------------
                    // 7.4 Xóa CV
                    // FK: cv.candidate_id
                    // -> candidates.id
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM cv WHERE candidate_id = ?",
                            candidateId
                    );

                    // ---------------------------------------------
                    // 7.5 Xóa wishlist
                    // FK: wishlist.candidate_id
                    // -> candidates.id
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM wishlist WHERE candidate_id = ?",
                            candidateId
                    );

                    // ---------------------------------------------
                    // 7.6 Cuối cùng xóa candidate
                    // ---------------------------------------------
                    jdbcTemplate.update(
                            "DELETE FROM candidates WHERE id = ?",
                            candidateId
                    );
                }
            }

            // =====================================================
            // 8. Cuối cùng xóa USER
            // =====================================================
            int deleted = jdbcTemplate.update(
                    "DELETE FROM users WHERE id = ?",
                    userId
            );

            if (deleted == 0) {
                throw new IllegalArgumentException(
                        "Không tìm thấy tài khoản."
                );
            }

        } catch (DataIntegrityViolationException e) {

            throw new IllegalStateException(
                    "Không thể xóa tài khoản vì tài khoản này vẫn còn dữ liệu liên quan."
            );
        }
    }


    // ===== THỐNG KÊ NGƯỜI DÙNG =====

    public long countAll() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );

        return count == null ? 0 : count;
    }

    public long countByRole(UserRole role) {
        if (role == null) {
            return 0;
        }

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = ?",
                Long.class,
                role.name()
        );

        return count == null ? 0 : count;
    }

    public long countByStatus(UserStatus status) {
        if (status == null) {
            return 0;
        }

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE status = ?",
                Long.class,
                status.name()
        );

        return count == null ? 0 : count;
    }

    // ===== SỐ NGƯỜI DÙNG ĐĂNG KÝ TRONG KHOẢNG THỜI GIAN =====

    public long countBetween(LocalDateTime from, LocalDateTime to) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM users
                        WHERE created_at >= ?
                          AND created_at < ?
                        """,
                Long.class,
                Timestamp.valueOf(from),
                Timestamp.valueOf(to)
        );

        return count == null ? 0 : count;
    }
}
