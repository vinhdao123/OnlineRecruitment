package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

// =====================================================
// REPOSITORY ĐẶT LỊCH PHỎNG VẤN PHÍA ỨNG VIÊN
// Chứa các câu SQL đọc đơn ứng tuyển, lịch đã đặt và khung giờ
// phỏng vấn cho ứng viên. Việc INSERT lịch phỏng vấn vẫn dùng
// InterviewRepository.bookInterview (dùng chung với recruiter).
// =====================================================

@Repository
public class CandidateInterviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public CandidateInterviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy thông tin đơn ứng tuyển (kèm job, công ty, recruiter)
    // chỉ khi đơn thuộc ứng viên có username / email đang đăng nhập
    public List<Map<String, Object>> findApplicationOfLoginUser(
            Long applicationId, String loginName) {

        String sql = """
            SELECT
                a.id AS applicationId,
                a.status AS applicationStatus,
                a.job_id AS jobId,
                j.title AS jobTitle,
                j.company_id AS companyId,
                j.recruiter_id AS recruiterId,
                ca.id AS candidateId,
                c.company_name AS companyName
            FROM dbo.applications a
            INNER JOIN dbo.candidates ca
                ON ca.id = a.candidate_id
            INNER JOIN dbo.users u
                ON u.id = ca.user_id
            INNER JOIN dbo.jobs j
                ON j.id = a.job_id
            LEFT JOIN dbo.companies c
                ON c.id = j.company_id
            WHERE a.id = ?
              AND (u.email = ? OR u.username = ?)
            """;

        return jdbcTemplate.queryForList(
                sql,
                applicationId,
                loginName,
                loginName
        );
    }

    // Đếm số lịch SCHEDULED của đơn ứng tuyển
    public int countScheduledInterviews(Long applicationId, Long candidateId) {

        String sql = """
            SELECT COUNT(*)
            FROM dbo.interview_schedules
            WHERE application_id = ?
              AND candidate_id = ?
              AND status = 'SCHEDULED'
            """;

        Integer booked = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                applicationId,
                candidateId
        );

        return booked == null ? 0 : booked;
    }

    // Lấy các khung giờ ứng viên có thể chọn:
    // - Thuộc đúng công ty + recruiter của job
    // - Nằm trong tương lai
    // - OPEN, hoặc BOOKED do chính ứng viên này đặt
    // - Không xung đột với lịch đã đặt (xem chú thích trong SQL)
    public List<Map<String, Object>> findAvailableSlots(
            Long companyId, Long recruiterId, Long candidateId) {

        String sql = """
            SELECT
                ia.id AS id,
                ia.available_date AS available_date,
                ia.start_time AS start_time,
                ia.end_time AS end_time,
                ia.status AS status

            FROM dbo.interview_availability ia

            WHERE ia.company_id = ?
              AND ia.recruiter_id = ?

              -- Chỉ lấy khung giờ trong tương lai
              AND (
                    ia.available_date > CAST(GETDATE() AS date)
                    OR (
                        ia.available_date = CAST(GETDATE() AS date)
                        AND ia.start_time > CAST(GETDATE() AS time)
                    )
              )

              -- Chỉ hiển thị:
              -- 1. Slot OPEN
              -- 2. Slot BOOKED do chính candidate này đặt
              AND (
                    ia.status = 'OPEN'
                    OR (
                        ia.status = 'BOOKED'
                        AND EXISTS (
                            SELECT 1
                            FROM dbo.interview_schedules own
                            WHERE own.availability_id = ia.id
                              AND own.candidate_id = ?
                              AND own.company_id = ia.company_id
                              AND own.status = 'SCHEDULED'
                        )
                    )
              )

              -- Loại các khung giờ bị xung đột
              AND NOT EXISTS (
                    SELECT 1
                    FROM dbo.interview_schedules s
                    INNER JOIN dbo.interview_availability booked
                        ON booked.id = s.availability_id

                    WHERE s.status = 'SCHEDULED'
                      AND booked.available_date = ia.available_date

                      AND (
                            -- Cùng công ty, ứng viên khác:
                            -- không được trùng thời gian
                            (
                                s.candidate_id <> ?
                                AND booked.company_id = ia.company_id
                                AND booked.start_time < ia.end_time
                                AND booked.end_time > ia.start_time
                            )

                            OR

                            -- Khác công ty, cùng ứng viên:
                            -- không được trùng thời gian
                            (
                                s.candidate_id = ?
                                AND booked.company_id <> ia.company_id
                                AND booked.start_time < ia.end_time
                                AND booked.end_time > ia.start_time
                            )

                            OR

                            -- Cùng công ty, cùng ứng viên,
                            -- cùng ngày nhưng khác khung giờ:
                            -- không được chọn
                            (
                                s.candidate_id = ?
                                AND booked.company_id = ia.company_id
                                AND (
                                    booked.start_time <> ia.start_time
                                    OR booked.end_time <> ia.end_time
                                )
                            )
                      )
              )

            ORDER BY ia.available_date, ia.start_time
            """;

        return jdbcTemplate.queryForList(
                sql,
                companyId,
                recruiterId,
                candidateId,
                candidateId,
                candidateId,
                candidateId
        );
    }

    // Lấy candidate_id theo username / email đang đăng nhập
    public List<Long> findCandidateIdsByLoginName(String loginName) {

        String sql = """
            SELECT ca.id
            FROM dbo.candidates ca
            INNER JOIN dbo.users u
                ON u.id = ca.user_id
            WHERE u.username = ?
               OR u.email = ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                loginName,
                loginName
        );
    }

    // Lấy recruiter và company của đơn ACCEPTED thuộc ứng viên
    public List<Map<String, Object>> findAcceptedApplicationJob(
            Long applicationId, Long candidateId) {

        String sql = """
            SELECT
                j.recruiter_id,
                j.company_id
            FROM dbo.applications a
            INNER JOIN dbo.jobs j
                ON j.id = a.job_id
            WHERE a.id = ?
              AND a.candidate_id = ?
              AND a.status = 'ACCEPTED'
            """;

        return jdbcTemplate.queryForList(
                sql,
                applicationId,
                candidateId
        );
    }

    // Kiểm tra khung giờ có thuộc đúng công ty và recruiter không
    public boolean existsAvailability(
            Long availabilityId, Long companyId, Long recruiterId) {

        String sql = """
            SELECT COUNT(*)
            FROM dbo.interview_availability
            WHERE id = ?
              AND company_id = ?
              AND recruiter_id = ?
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                availabilityId,
                companyId,
                recruiterId
        );

        return count != null && count > 0;
    }
}
