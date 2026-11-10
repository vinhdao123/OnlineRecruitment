
package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.InterviewRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class CandidateInterviewController {

    private final JdbcTemplate jdbcTemplate;
    private final InterviewRepository interviewRepository;

    public CandidateInterviewController(
            JdbcTemplate jdbcTemplate,
            InterviewRepository interviewRepository
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.interviewRepository = interviewRepository;
    }

    // =====================================================
    // 1. HIỂN THỊ KHUNG GIỜ PHỎNG VẤN
    // =====================================================

    @GetMapping("/candidate/interviews/{applicationId}")
    public String showInterviewSlots(
            @PathVariable Long applicationId,
            Authentication authentication,
            Model model
    ) {

        // Kiểm tra đăng nhập
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        String loginName = authentication.getName();

        // =================================================
        // LẤY THÔNG TIN ĐƠN ỨNG TUYỂN
        // =================================================

        String applicationSql = """
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

        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(
                        applicationSql,
                        applicationId,
                        loginName,
                        loginName
                );

        // Không tìm thấy đơn hoặc không thuộc ứng viên
        if (applications.isEmpty()) {
            return "redirect:/candidate/applications";
        }

        Map<String, Object> application = applications.get(0);

        // Chỉ đơn ACCEPTED mới được đặt lịch
        if (!"ACCEPTED".equalsIgnoreCase(
                String.valueOf(application.get("applicationStatus")))) {
            return "redirect:/candidate/applications";
        }

        Long candidateId = ((Number)
                application.get("candidateId")).longValue();

        Long companyId = ((Number)
                application.get("companyId")).longValue();

        Long recruiterId = ((Number)
                application.get("recruiterId")).longValue();

        // =================================================
        // KIỂM TRA ĐƠN NÀY ĐÃ CÓ LỊCH CHƯA
        // =================================================

        String bookedSql = """
            SELECT COUNT(*)
            FROM dbo.interview_schedules
            WHERE application_id = ?
              AND candidate_id = ?
              AND status = 'SCHEDULED'
            """;

        Integer booked = jdbcTemplate.queryForObject(
                bookedSql,
                Integer.class,
                applicationId,
                candidateId
        );

        if (booked != null && booked > 0) {
            return "redirect:/candidate/applications";
        }

        // =================================================
        // LỌC KHUNG GIỜ
        // =================================================

        String slotsSql = """
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

        List<Map<String, Object>> slots =
                jdbcTemplate.queryForList(
                        slotsSql,
                        companyId,
                        recruiterId,
                        candidateId,
                        candidateId,
                        candidateId,
                        candidateId
                );

        // =================================================
        // GỬI DỮ LIỆU SANG THYMELEAF
        // =================================================

        model.addAttribute("application", application);
        model.addAttribute("applicationId", applicationId);
        model.addAttribute("slots", slots);

        return "candidate/interview-booking";
    }

    // =====================================================
    // 2. XÁC NHẬN ĐẶT LỊCH PHỎNG VẤN
    // =====================================================

    @PostMapping("/candidate/interviews/book")
    public String bookInterview(
            @RequestParam Long applicationId,
            @RequestParam Long availabilityId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        // Kiểm tra đăng nhập
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        String loginName = authentication.getName();

        // =================================================
        // LẤY CANDIDATE ID
        // =================================================

        String candidateSql = """
            SELECT ca.id
            FROM dbo.candidates ca
            INNER JOIN dbo.users u
                ON u.id = ca.user_id
            WHERE u.username = ?
               OR u.email = ?
            """;

        List<Long> candidateIds = jdbcTemplate.query(
                candidateSql,
                (rs, rowNum) -> rs.getLong("id"),
                loginName,
                loginName
        );

        if (candidateIds.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không tìm thấy tài khoản ứng viên."
            );

            return "redirect:/candidate/applications";
        }

        Long candidateId = candidateIds.get(0);

        // =================================================
        // LẤY RECRUITER VÀ COMPANY TỪ ĐƠN
        // =================================================

        String applicationSql = """
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

        List<Map<String, Object>> applications =
                jdbcTemplate.queryForList(
                        applicationSql,
                        applicationId,
                        candidateId
                );

        if (applications.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Đơn ứng tuyển không hợp lệ hoặc chưa được chấp nhận."
            );

            return "redirect:/candidate/applications";
        }

        Map<String, Object> application = applications.get(0);

        Long recruiterId = ((Number)
                application.get("recruiter_id")).longValue();

        Long companyId = ((Number)
                application.get("company_id")).longValue();

        // =================================================
        // KIỂM TRA KHUNG GIỜ THUỘC ĐÚNG CÔNG TY
        // VÀ RECRUITER
        // =================================================

        String availabilitySql = """
            SELECT COUNT(*)
            FROM dbo.interview_availability
            WHERE id = ?
              AND company_id = ?
              AND recruiter_id = ?
            """;

        Integer availabilityCount = jdbcTemplate.queryForObject(
                availabilitySql,
                Integer.class,
                availabilityId,
                companyId,
                recruiterId
        );

        if (availabilityCount == null || availabilityCount == 0) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Khung giờ phỏng vấn không hợp lệ."
            );

            return "redirect:/candidate/applications";
        }

        // =================================================
        // THỰC HIỆN ĐẶT LỊCH
        // Repository sẽ kiểm tra lại toàn bộ quy tắc
        // trong transaction trước khi INSERT.
        // =================================================

        try {

            int result = interviewRepository.bookInterview(
                    applicationId,
                    availabilityId,
                    candidateId,
                    recruiterId,
                    companyId
            );

            if (result > 0) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Đặt lịch phỏng vấn thành công!"
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Không thể đặt lịch. Vui lòng thử lại."
                );
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể đặt lịch. Khung giờ có thể đã bị trùng hoặc được người khác chọn."
            );
        }

        return "redirect:/candidate/applications";
    }
}
