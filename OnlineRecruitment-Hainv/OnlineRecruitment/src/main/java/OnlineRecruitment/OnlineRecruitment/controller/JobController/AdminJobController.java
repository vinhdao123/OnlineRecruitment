package OnlineRecruitment.OnlineRecruitment.controller.JobController;

import OnlineRecruitment.OnlineRecruitment.service.serviceJob.JobService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/jobs")
public class AdminJobController {

    private static final int MAX_REASON = 500;

    private final JobService jobService;

    public AdminJobController(JobService jobService) {
        this.jobService = jobService;
    }

    // ===== DANH SÁCH TIN CHỜ DUYỆT =====

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        model.addAttribute(
                "jobPage",
                jobService.findForReview(keyword, status, page)
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("activeMenu", "jobs");

        // File: templates/Job/admin-jobs.html
        return "Job/admin-jobs";
    }

    // ===== DUYỆT TIN =====

    @PostMapping("/{id}/approve")
    public String approve(
            @PathVariable Long id,
            RedirectAttributes ra) {

        jobService.approve(id);

        ra.addFlashAttribute(
                "success",
                "Đã duyệt tin. Tin đã hiển thị công khai trên trang việc làm."
        );

        return "redirect:/admin/jobs";
    }

    // ===== TỪ CHỐI TIN =====

    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @RequestParam String reason,
            RedirectAttributes ra) {

        if (reason == null || reason.isBlank()) {
            ra.addFlashAttribute(
                    "error",
                    "Vui lòng nhập lý do từ chối."
            );

            return "redirect:/jobs/" + id;
        }

        if (reason.trim().length() > MAX_REASON) {
            ra.addFlashAttribute(
                    "error",
                    "Lý do từ chối tối đa " + MAX_REASON + " ký tự."
            );

            return "redirect:/jobs/" + id;
        }

        jobService.reject(id, reason);

        ra.addFlashAttribute(
                "success",
                "Đã từ chối tin và gửi lý do cho nhà tuyển dụng."
        );

        return "redirect:/admin/jobs";
    }
}