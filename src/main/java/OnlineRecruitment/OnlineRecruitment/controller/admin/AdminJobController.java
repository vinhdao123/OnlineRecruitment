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
            @RequestParam(required = false) String status,
            Model model
    ) {
        // The original Admin design supports all statuses and search.
        // Keep the existing JobService-only constructor for integration tests.
        String selectedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!selectedStatus.isEmpty()) {
            try {
                OnlineRecruitment.OnlineRecruitment.entity.JobStatus.valueOf(selectedStatus);
            } catch (IllegalArgumentException ex) {
                selectedStatus = "";
            }
        }
        List<Job> jobs = jobService.findAllJobsForManagement(keyword, selectedStatus);
        model.addAttribute("jobs", jobs);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isAdmin", true);
        model.addAttribute("selectedStatus", selectedStatus);
        model.addAttribute("activeMenu", "PENDING".equals(selectedStatus) ? "jobs-pending" :
                "CLOSED".equals(selectedStatus) ? "jobs-closed" :
                "REJECTED".equals(selectedStatus) ? "jobs-rejected" : "jobs");

        return "admin/jobs";
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
