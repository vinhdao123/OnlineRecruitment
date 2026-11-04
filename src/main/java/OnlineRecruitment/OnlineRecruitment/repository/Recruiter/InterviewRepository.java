
package OnlineRecruitment.OnlineRecruitment.repository.Recruiter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Repository
public class InterviewRepository {

    private final JdbcTemplate jdbcTemplate;

    public InterviewRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Lấy ID nhà tuyển dụng từ username hoặc email
    public Long findRecruiterId(String loginName) {
        String sql = """
            SELECT id
            FROM users
            WHERE (username = ? OR email = ?)
              AND role = 'RECRUITER'
              AND status = 'ACTIVE'
            """;

        List<Long> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                loginName,
                loginName
        );

        return result.isEmpty() ? null : result.get(0);
    }

    // Lấy công ty của nhà tuyển dụng
    public Long findCompanyId(Long recruiterId) {
        String sql = """
            SELECT TOP 1 id
            FROM companies
            WHERE recruiter_id = ?
            ORDER BY id DESC
            """;

        List<Long> result = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("id"),
                recruiterId
        );

        return result.isEmpty() ? null : result.get(0);
    }

    // Thêm một khung giờ phỏng vấn
    public int insertAvailability(
            Long recruiterId,
            Long companyId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        String sql = """
            IF NOT EXISTS (
                SELECT 1
                FROM interview_availability
                WHERE recruiter_id = ?
                  AND company_id = ?
                  AND available_date = CAST(? AS date)
                  AND start_time = CAST(? AS time)
            )
            BEGIN
                INSERT INTO interview_availability (
                    recruiter_id,
                    company_id,
                    available_date,
                    start_time,
                    end_time,
                    status
                )
                VALUES (
                    ?, ?,
                    CAST(? AS date),
                    CAST(? AS time),
                    CAST(? AS time),
                    'OPEN'
                );
            END
            """;

        return jdbcTemplate.update(
                sql,
                recruiterId,
                companyId,
                date.toString(),
                startTime.toString(),
                recruiterId,
                companyId,
                date.toString(),
                startTime.toString(),
                endTime.toString()
        );
    }

    // Lấy tất cả lịch của nhà tuyển dụng
    public List<Map<String, Object>> findAvailability(
            Long recruiterId,
            Long companyId
    ) {
        String sql = """
            SELECT
                id,
                available_date,
                start_time,
                end_time,
                status
            FROM interview_availability
            WHERE recruiter_id = ?
              AND company_id = ?
            ORDER BY available_date, start_time
            """;

        return jdbcTemplate.queryForList(
                sql,
                recruiterId,
                companyId
        );
    }

    // Lấy các ngày đã tạo lịch
    public List<LocalDate> findExistingDates(
            Long recruiterId,
            Long companyId
    ) {
        String sql = """
            SELECT DISTINCT available_date
            FROM interview_availability
            WHERE recruiter_id = ?
              AND company_id = ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) ->
                        rs.getDate("available_date").toLocalDate(),
                recruiterId,
                companyId
        );
    }

    // Lấy các khung giờ còn trống
    // Lấy các khung giờ còn trống và chưa đến thời điểm phỏng vấn
    public List<Map<String, Object>> findOpenSlots(
            Long recruiterId,
            Long companyId
    ) {
        String sql = """
        SELECT
            id,
            available_date,
            start_time,
            end_time,
            status
        FROM interview_availability
        WHERE recruiter_id = ?
          AND company_id = ?
          AND status = 'OPEN'
          AND (
                available_date > CAST(GETDATE() AS date)
                OR (
                    available_date = CAST(GETDATE() AS date)
                    AND start_time > CAST(GETDATE() AS time)
                )
          )
        ORDER BY available_date, start_time
        """;

        return jdbcTemplate.queryForList(
                sql,
                recruiterId,
                companyId
        );
    }

    // Đặt lịch phỏng vấn
    public int bookInterview(
            Long applicationId,
            Long availabilityId,
            Long candidateId,
            Long recruiterId,
            Long companyId
    ) {

        String sql = """
            BEGIN TRY
                BEGIN TRANSACTION;

                -- Kiểm tra đơn ứng tuyển thuộc đúng Candidate,
                -- đúng Recruiter và đúng công ty
                IF NOT EXISTS (
                    SELECT 1
                    FROM applications a
                    INNER JOIN jobs j ON j.id = a.job_id
                    WHERE a.id = ?
                      AND a.candidate_id = ?
                      AND a.status = 'ACCEPTED'
                      AND j.recruiter_id = ?
                      AND j.company_id = ?
                )
                BEGIN
                    THROW 50001, N'Đơn ứng tuyển không hợp lệ.', 1;
                END;

                -- Không cho một đơn có hai lịch đang hoạt động
                IF EXISTS (
                    SELECT 1
                    FROM interview_schedules
                    WHERE application_id = ?
                      AND status = 'SCHEDULED'
                )
                BEGIN
                    THROW 50002, N'Đơn này đã có lịch phỏng vấn.', 1;
                END;

                -- Chỉ đặt được khung giờ còn trống
                UPDATE interview_availability
                SET status = 'BOOKED'
                WHERE id = ?
                  AND recruiter_id = ?
                  AND company_id = ?
                  AND status = 'OPEN'
                  AND (
                        available_date > CAST(GETDATE() AS date)
                        OR (
                            available_date = CAST(GETDATE() AS date)
                            AND start_time > CAST(GETDATE() AS time)
                        )
                  );

                IF @@ROWCOUNT = 0
                BEGIN
                    THROW 50003, N'Khung giờ không còn trống.', 1;
                END;

                -- Lưu lịch phỏng vấn
                INSERT INTO interview_schedules (
                    application_id,
                    availability_id,
                    candidate_id,
                    recruiter_id,
                    company_id,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    'SCHEDULED',
                    GETDATE(),
                    GETDATE()
                );

                COMMIT TRANSACTION;

                SELECT 1 AS result;

            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0
                    ROLLBACK TRANSACTION;

                THROW;
            END CATCH;
            """;

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                applicationId,
                candidateId,
                recruiterId,
                companyId,
                applicationId,
                availabilityId,
                recruiterId,
                companyId,
                applicationId,
                availabilityId,
                candidateId,
                recruiterId,
                companyId
        );
    }
    // Lấy danh sách công việc của Recruiter
    public List<Map<String, Object>> findRecruiterJobs(Long recruiterId, Long companyId) {
        String sql = """
        SELECT id, title
        FROM jobs
        WHERE recruiter_id = ?
          AND company_id = ?
        ORDER BY title
        """;

        return jdbcTemplate.queryForList(sql, recruiterId, companyId);
    }

    // Lấy danh sách lịch phỏng vấn đã được ứng viên đặt
//    public List<Map<String, Object>> findBookedInterviews(
//            Long recruiterId,
//            Long companyId,
//            Long jobId,
//            LocalDate interviewDate
//    ) {
//        String sql = """
//        SELECT
//            s.id AS scheduleId,
//            a.id AS applicationId,
//            c.id AS candidateId,
//            u.username AS candidateName,
//            u.email AS email,
//            j.id AS jobId,
//            j.title AS jobTitle,
//            av.available_date AS interviewDate,
//            av.start_time AS startTime,
//            av.end_time AS endTime,
//            s.status AS scheduleStatus,
//            s.note AS interviewNote
//        FROM interview_schedules s
//        INNER JOIN applications a
//            ON a.id = s.application_id
//        INNER JOIN candidates c
//            ON c.id = s.candidate_id
//        INNER JOIN users u
//            ON u.id = c.user_id
//        INNER JOIN jobs j
//            ON j.id = a.job_id
//        INNER JOIN interview_availability av
//            ON av.id = s.availability_id
//        WHERE s.recruiter_id = ?
//          AND s.company_id = ?
//          AND s.status = 'SCHEDULED'
//          AND (? IS NULL OR j.id = ?)
//          AND (? IS NULL OR av.available_date = ?)
//        ORDER BY av.available_date, av.start_time
//        """;
//
//        return jdbcTemplate.queryForList(
//                sql,
//                recruiterId,
//                companyId,
//                jobId, jobId,
//                interviewDate, interviewDate
//        );
//    }
    // Lấy danh sách lịch phỏng vấn đã được ứng viên đặt
    public List<Map<String, Object>> findBookedInterviews(
            Long recruiterId,
            Long companyId,
            Long jobId,
            LocalDate interviewDate
    ) {
        String sql = """
        SELECT
            s.id AS scheduleId,
            a.id AS applicationId,
            c.id AS candidateId,
            u.username AS candidateName,
            u.email AS email,
            j.id AS jobId,
            j.title AS jobTitle,
            av.available_date AS interviewDate,
            av.start_time AS startTime,
            av.end_time AS endTime,
            s.status AS scheduleStatus,
            s.note AS interviewNote
        FROM interview_schedules s
        INNER JOIN applications a
            ON a.id = s.application_id
        INNER JOIN candidates c
            ON c.id = s.candidate_id
        INNER JOIN users u
            ON u.id = c.user_id
        INNER JOIN jobs j
            ON j.id = a.job_id
        INNER JOIN interview_availability av
            ON av.id = s.availability_id
        WHERE s.recruiter_id = ?
          AND s.company_id = ?
          AND s.status = 'SCHEDULED'
          AND (? = 0 OR j.id = ?)
          AND av.available_date = ?
        ORDER BY av.available_date, av.start_time
        """;

        return jdbcTemplate.queryForList(
                sql,
                recruiterId,
                companyId,
                jobId == null ? 0L : jobId,
                jobId == null ? 0L : jobId,
                interviewDate
        );
    }

    // Lấy tất cả khung giờ trong ngày, gồm OPEN, BOOKED và CLOSED
    public List<Map<String, Object>> findDailyInterviewSlots(
            Long recruiterId,
            Long companyId,
            LocalDate interviewDate
    ) {
        String sql = """
        SELECT
            av.id AS availabilityId,
            av.available_date AS interviewDate,
            av.start_time AS startTime,
            av.end_time AS endTime,
            av.status AS slotStatus,

            CASE
                WHEN av.status = 'BOOKED' THEN 'BOOKED'

                WHEN av.available_date < CAST(GETDATE() AS date)
                     OR (
                         av.available_date = CAST(GETDATE() AS date)
                         AND av.start_time <= CAST(GETDATE() AS time)
                     )
                THEN 'CLOSED'

                ELSE 'OPEN'
            END AS displayStatus,

            u.username AS candidateName,
            a.id AS applicationId,
            j.title AS jobTitle

        FROM interview_availability av

        LEFT JOIN interview_schedules s
            ON s.availability_id = av.id
           AND s.status = 'SCHEDULED'

        LEFT JOIN applications a
            ON a.id = s.application_id

        LEFT JOIN candidates c
            ON c.id = s.candidate_id

        LEFT JOIN users u
            ON u.id = c.user_id

        LEFT JOIN jobs j
            ON j.id = a.job_id

        WHERE av.recruiter_id = ?
          AND av.company_id = ?
          AND av.available_date = ?

        ORDER BY av.start_time
        """;

        return jdbcTemplate.queryForList(
                sql,
                recruiterId,
                companyId,
                interviewDate
        );
    }
}
