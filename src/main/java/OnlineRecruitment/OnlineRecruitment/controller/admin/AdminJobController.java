package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobController {

    private static final int MAX_REASON_LENGTH = 500;

    private final JobService jobService;

    /**
     * Hiển thị danh sách tin đang chờ Admin duyệt.
     * Tìm kiếm theo tiêu đề, địa điểm hoặc email nhà tuyển dụng.
     */
    @GetMapping
    // Liệt kê jobs trong luồng tuyển dụng.
    public String listJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "") String status,
            Model model
    ) {
        jobService.refreshExpiredJobs();
        status = status.trim().toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("", "PENDING", "ACTIVE", "REJECTED", "CLOSED", "EXPIRED", "DRAFT").contains(status)) {
            status = "";
        }
        List<Job> jobs = jobService.findAllJobsForManagement(null, null, status);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String normalizedKeyword =
                    keyword.trim().toLowerCase(Locale.ROOT);

            jobs = jobs.stream()
                    .filter(job ->
                            (job.getTitle() != null
                                    && job.getTitle()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(normalizedKeyword))
                                    ||
                                    (job.getLocation() != null
                                            && job.getLocation()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(normalizedKeyword))
                                    ||
                                    (job.getRecruiter() != null
                                            && job.getRecruiter().getEmail() != null
                                            && job.getRecruiter().getEmail()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(normalizedKeyword))
                    )
                    .collect(Collectors.toList());
        }

        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isAdmin", true);
        model.addAttribute("status", status);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeMenu", status.isEmpty() ? "jobs" : "jobs-" + status.toLowerCase(Locale.ROOT));

        return "admin/jobs";
    }

    @PostMapping("/{id}/reopen")
    public String reopenJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            jobService.reopenAdminJob(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển tin về nháp. Nhà tuyển dụng cần cập nhật hạn và gửi duyệt lại.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/jobs?status=DRAFT";
    }

    /**
     * Duyệt tin tuyển dụng.
     */
    @PostMapping("/{id}/approve")
    // Duyệt job trong luồng tuyển dụng.
    public String approveJob(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            jobService.approveJob(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã duyệt tin tuyển dụng thành công."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/jobs";
    }

    /**
     * Từ chối tin tuyển dụng.
     */
    @PostMapping("/{id}/reject")
    // Từ chối job trong luồng tuyển dụng.
    public String rejectJob(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            RedirectAttributes redirectAttributes
    ) {
        if (reason == null || reason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Vui lòng nhập lý do từ chối."
            );

            return "redirect:/admin/jobs";
        }

        reason = reason.trim();

        if (reason.length() > MAX_REASON_LENGTH) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Lý do từ chối không được vượt quá 500 ký tự."
            );

            return "redirect:/admin/jobs";
        }

        try {
            jobService.rejectJob(id, reason);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã từ chối tin tuyển dụng."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/jobs";
    }
}
