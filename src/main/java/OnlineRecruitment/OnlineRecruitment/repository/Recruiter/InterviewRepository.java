
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

    // =====================================================
    // 1. LẤY ID NHÀ TUYỂN DỤNG
    // =====================================================

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

    // =====================================================
    // 2. LẤY ID CÔNG TY
    // =====================================================

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

    // =====================================================
    // 3. THÊM KHUNG GIỜ PHỎNG VẤN
    // =====================================================

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
                    ?,
                    ?,
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

    // =====================================================
    // 4. LẤY TẤT CẢ KHUNG GIỜ CỦA RECRUITER
    // =====================================================

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

    // =====================================================
    // 5. LẤY CÁC NGÀY ĐÃ TẠO LỊCH
    // =====================================================

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

    // =====================================================
    // 6. LẤY CÁC KHUNG GIỜ CÒN TRỐNG
    // Dùng cho các luồng đang yêu cầu slot OPEN.
    // =====================================================

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

    // =====================================================
    // 7. ĐẶT LỊCH PHỎNG VẤN
    //
    // Quy tắc:
    // - Cùng candidate, company, ngày và giờ:
    //   cho phép dùng chung slot cho nhiều application.
    //
    // - Cùng candidate, company, cùng ngày khác giờ:
    //   không cho phép.
    //
    // - Khác candidate, cùng company, trùng giờ:
    //   không cho phép.
    //
    // - Cùng candidate, khác company, trùng giờ:
    //   không cho phép.
    // =====================================================

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

                DECLARE @applicationId BIGINT = ?;
                DECLARE @availabilityId BIGINT = ?;
                DECLARE @candidateId BIGINT = ?;
                DECLARE @recruiterId BIGINT = ?;
                DECLARE @companyId BIGINT = ?;

                DECLARE @interviewDate DATE;
                DECLARE @startTime TIME;
                DECLARE @endTime TIME;
                DECLARE @slotStatus VARCHAR(30);

                -- =========================================
                -- 1. KHÓA COMPANY
                -- Tuần tự hóa các thao tác đặt lịch cùng công ty.
                -- =========================================

                IF NOT EXISTS (
                    SELECT id
                    FROM dbo.companies WITH (UPDLOCK, HOLDLOCK)
                    WHERE id = @companyId
                )
                BEGIN
                    THROW 50001, N'Công ty không hợp lệ.', 1;
                END;

                -- =========================================
                -- 2. KHÓA CANDIDATE
                -- Hạn chế hai thao tác đặt lịch đồng thời
                -- của cùng một ứng viên.
                -- =========================================

                IF NOT EXISTS (
                    SELECT id
                    FROM dbo.candidates WITH (UPDLOCK, HOLDLOCK)
                    WHERE id = @candidateId
                )
                BEGIN
                    THROW 50002, N'Ứng viên không hợp lệ.', 1;
                END;

                -- =========================================
                -- 3. KIỂM TRA ĐƠN ỨNG TUYỂN
                -- =========================================

                IF NOT EXISTS (
                    SELECT 1
                    FROM dbo.applications a
                    INNER JOIN dbo.jobs j
                        ON j.id = a.job_id
                    WHERE a.id = @applicationId
                      AND a.candidate_id = @candidateId
                      AND a.status = 'ACCEPTED'
                      AND j.recruiter_id = @recruiterId
                      AND j.company_id = @companyId
                )
                BEGIN
                    THROW 50003,
                        N'Đơn ứng tuyển không hợp lệ hoặc chưa được chấp nhận.',
                        1;
                END;

                -- =========================================
                -- 4. MỖI APPLICATION CHỈ CÓ MỘT LỊCH
                -- ĐANG SCHEDULED
                -- =========================================

                IF EXISTS (
                    SELECT 1
                    FROM dbo.interview_schedules
                    WHERE application_id = @applicationId
                      AND status = 'SCHEDULED'
                )
                BEGIN
                    THROW 50004,
                        N'Đơn ứng tuyển này đã có lịch phỏng vấn.',
                        1;
                END;

                -- =========================================
                -- 5. LẤY VÀ KHÓA KHUNG GIỜ
                -- =========================================

                SELECT
                    @interviewDate = available_date,
                    @startTime = start_time,
                    @endTime = end_time,
                    @slotStatus = status
                FROM dbo.interview_availability
                    WITH (UPDLOCK, HOLDLOCK)
                WHERE id = @availabilityId
                  AND recruiter_id = @recruiterId
                  AND company_id = @companyId;

                IF @interviewDate IS NULL
                BEGIN
                    THROW 50005,
                        N'Khung giờ phỏng vấn không hợp lệ.',
                        1;
                END;

                -- =========================================
                -- 6. KHÔNG CHO ĐẶT LỊCH TRONG QUÁ KHỨ
                -- =========================================

                IF (
                    @interviewDate < CAST(GETDATE() AS DATE)
                    OR (
                        @interviewDate = CAST(GETDATE() AS DATE)
                        AND @startTime <= CAST(GETDATE() AS TIME)
                    )
                )
                BEGIN
                    THROW 50006,
                        N'Khung giờ phỏng vấn đã qua.',
                        1;
                END;

                -- =========================================
                -- 7. KIỂM TRA TRẠNG THÁI SLOT
                -- OPEN: được đặt lần đầu.
                -- BOOKED: chỉ candidate đã đặt slot này
                -- mới được dùng lại cho application khác.
                -- =========================================

                IF @slotStatus = 'BOOKED'
                   AND NOT EXISTS (
                        SELECT 1
                        FROM dbo.interview_schedules s
                        WHERE s.availability_id = @availabilityId
                          AND s.candidate_id = @candidateId
                          AND s.status = 'SCHEDULED'
                   )
                BEGIN
                    THROW 50007,
                        N'Khung giờ đã được ứng viên khác đặt.',
                        1;
                END;

                IF @slotStatus IS NULL
                   OR @slotStatus NOT IN ('OPEN', 'BOOKED')
                BEGIN
                    THROW 50008,
                        N'Khung giờ không còn khả dụng.',
                        1;
                END;

                -- =========================================
                -- 8. CÙNG CÔNG TY, KHÁC CANDIDATE
                -- KHÔNG ĐƯỢC TRÙNG THỜI GIAN
                -- =========================================

                IF EXISTS (
                    SELECT 1
                    FROM dbo.interview_schedules s
                    INNER JOIN dbo.interview_availability booked
                        ON booked.id = s.availability_id
                    WHERE s.status = 'SCHEDULED'
                      AND s.candidate_id <> @candidateId
                      AND booked.company_id = @companyId
                      AND booked.available_date = @interviewDate
                      AND booked.start_time < @endTime
                      AND booked.end_time > @startTime
                )
                BEGIN
                    THROW 50009,
                        N'Khung giờ đã được ứng viên khác đặt.',
                        1;
                END;

                -- =========================================
                -- 9. KHÁC CÔNG TY, CÙNG CANDIDATE
                -- KHÔNG ĐƯỢC TRÙNG THỜI GIAN
                -- =========================================

                IF EXISTS (
                    SELECT 1
                    FROM dbo.interview_schedules s
                    INNER JOIN dbo.interview_availability booked
                        ON booked.id = s.availability_id
                    WHERE s.status = 'SCHEDULED'
                      AND s.candidate_id = @candidateId
                      AND booked.company_id <> @companyId
                      AND booked.available_date = @interviewDate
                      AND booked.start_time < @endTime
                      AND booked.end_time > @startTime
                )
                BEGIN
                    THROW 50010,
                        N'Bạn đã có lịch phỏng vấn trùng giờ tại công ty khác.',
                        1;
                END;

                -- =========================================
                -- 10. CÙNG CÔNG TY, CÙNG CANDIDATE
                -- CÙNG NGÀY NHƯNG KHÁC KHUNG GIỜ
                -- KHÔNG ĐƯỢC ĐẶT
                -- =========================================

                IF EXISTS (
                    SELECT 1
                    FROM dbo.interview_schedules s
                    INNER JOIN dbo.interview_availability booked
                        ON booked.id = s.availability_id
                    WHERE s.status = 'SCHEDULED'
                      AND s.candidate_id = @candidateId
                      AND booked.company_id = @companyId
                      AND booked.available_date = @interviewDate
                      AND (
                            booked.start_time <> @startTime
                            OR booked.end_time <> @endTime
                      )
                )
                BEGIN
                    THROW 50011,
                        N'Bạn đã có lịch khác tại công ty này trong cùng ngày.',
                        1;
                END;

                -- =========================================
                -- 11. CẬP NHẬT TRẠNG THÁI SLOT
                -- Chỉ cập nhật OPEN thành BOOKED.
                -- Nếu BOOKED và đúng candidate đã đặt:
                -- giữ nguyên BOOKED để dùng chung.
                -- =========================================

                IF @slotStatus = 'OPEN'
                BEGIN
                    UPDATE dbo.interview_availability
                    SET status = 'BOOKED'
                    WHERE id = @availabilityId
                      AND status = 'OPEN';

                    IF @@ROWCOUNT = 0
                    BEGIN
                        THROW 50012,
                            N'Khung giờ vừa được người khác đặt.',
                            1;
                    END;
                END;

                -- =========================================
                -- 12. THÊM LỊCH PHỎNG VẤN
                -- Cho phép nhiều application cùng availability
                -- nếu cùng candidate.
                -- =========================================

                INSERT INTO dbo.interview_schedules (
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
                    @applicationId,
                    @availabilityId,
                    @candidateId,
                    @recruiterId,
                    @companyId,
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
                availabilityId,
                candidateId,
                recruiterId,
                companyId
        );
    }

    // =====================================================
    // 8. LẤY DANH SÁCH CÔNG VIỆC CỦA RECRUITER
    // =====================================================

    public List<Map<String, Object>> findRecruiterJobs(
            Long recruiterId,
            Long companyId
    ) {
        String sql = """
            SELECT id, title
            FROM jobs
            WHERE recruiter_id = ?
              AND company_id = ?
            ORDER BY title
            """;

        return jdbcTemplate.queryForList(
                sql,
                recruiterId,
                companyId
        );
    }

    // =====================================================
    // 9. LẤY DANH SÁCH LỊCH PHỎNG VẤN ĐÃ ĐẶT
    // =====================================================

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

    // =====================================================
    // 10. LẤY TẤT CẢ KHUNG GIỜ TRONG NGÀY
    // Bao gồm OPEN, BOOKED và CLOSED.
    //
    // Lưu ý:
    // Phương thức này vẫn trả về từng application riêng.
    // Nếu một slot có nhiều application thì sẽ có nhiều dòng.
    // =====================================================

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
