
package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.InterviewRepository;
import OnlineRecruitment.OnlineRecruitment.service.InterviewService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recruiter/interviews")
public class RecruiterInterviewController {

    private final InterviewRepository interviewRepository;
    private final InterviewService interviewService;

    public RecruiterInterviewController(
            InterviewRepository interviewRepository,
            InterviewService interviewService
    ) {
        this.interviewRepository = interviewRepository;
        this.interviewService = interviewService;
    }

    // Lấy ID nhà tuyển dụng đang đăng nhập
    private Long getRecruiterId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Vui lòng đăng nhập.");
        }

        Long recruiterId = interviewRepository.findRecruiterId(
                authentication.getName()
        );

        if (recruiterId == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy tài khoản nhà tuyển dụng."
            );
        }

        return recruiterId;
    }

    // Lấy ID công ty
    private Long getCompanyId(Long recruiterId) {

        Long companyId = interviewRepository.findCompanyId(recruiterId);

        if (companyId == null) {
            throw new IllegalArgumentException(
                    "Bạn chưa có thông tin công ty."
            );
        }

        return companyId;
    }

    // Hiển thị trang tạo lịch
    @GetMapping("/availability")
    public String showAvailabilityPage(
            Authentication authentication,
            Model model
    ) {

        Long recruiterId = getRecruiterId(authentication);
        Long companyId = getCompanyId(recruiterId);

        model.addAttribute(
                "availability",
                interviewService.getAvailability(
                        recruiterId,
                        companyId
                )
        );

        // Lấy các ngày đã có lịch trong database
        List<LocalDate> existingDates =
                interviewRepository.findExistingDates(
                        recruiterId,
                        companyId
                );

        // Chuyển thành chuỗi yyyy-MM-dd
        String existingDateString = existingDates.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));

        model.addAttribute(
                "existingDateString",
                existingDateString
        );

        return "recruiter/recruitment_schedule";
    }

    // Lưu lịch phỏng vấn
    @PostMapping("/availability/save")
    public String saveAvailability(
            Authentication authentication,

            @RequestParam("dates")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            List<LocalDate> dates,

            @RequestParam("startTime")
            @DateTimeFormat(pattern = "HH:mm")
            LocalTime startTime,

            @RequestParam("endTime")
            @DateTimeFormat(pattern = "HH:mm")
            LocalTime endTime,

            @RequestParam("durationMinutes")
            Integer durationMinutes,

            RedirectAttributes redirectAttributes
    ) {

        try {

            Long recruiterId = getRecruiterId(authentication);
            Long companyId = getCompanyId(recruiterId);

            int inserted = interviewService.createAvailability(
                    recruiterId,
                    companyId,
                    dates,
                    startTime,
                    endTime,
                    durationMinutes
            );

            if (inserted > 0) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Đã tạo thành công " + inserted
                                + " khung giờ phỏng vấn."
                );
            }

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/recruiter/interviews/availability";
    }

    // Hiển thị trang xem lịch phỏng vấn
    @GetMapping
    public String showInterviewSchedule(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        Long recruiterId = getRecruiterId(authentication);
        Long companyId = getCompanyId(recruiterId);

        // Nếu chưa chọn ngày thì mặc định là hôm nay
        LocalDate selectedDate = (date != null)
                ? date
                : LocalDate.now();
        // Danh sách ngày đã được tạo khung giờ
        model.addAttribute(
                "availableDates",
                interviewRepository.findExistingDates(recruiterId, companyId)
        );


        // Danh sách công việc
        model.addAttribute(
                "jobs",
                interviewRepository.findRecruiterJobs(recruiterId, companyId)
        );

        // Danh sách ứng viên đã đặt lịch
        model.addAttribute(
                "interviews",
                interviewRepository.findBookedInterviews(
                        recruiterId,
                        companyId,
                        jobId,
                        selectedDate
                )
        );

        // Tất cả khung giờ trong ngày
        model.addAttribute(
                "dailySlots",
                interviewRepository.findDailyInterviewSlots(
                        recruiterId,
                        companyId,
                        selectedDate
                )
        );

        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("selectedDate", selectedDate);

        return "recruiter/interview-schedule";
    }
}
