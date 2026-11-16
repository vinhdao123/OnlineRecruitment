package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.service.RecruiterApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/recruiter/applications")
public class RecruiterApplicationController {

    private final RecruiterApplicationService applicationService;

    public RecruiterApplicationController(
            RecruiterApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // =========================================
    // DANH SÁCH ĐƠN ỨNG TUYỂN
    // =========================================

    @GetMapping
    public String applications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "SUBMITTED") String status,
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            Model model) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        Map<String, Object> data =
                applicationService.getApplications(
                        username, keyword, status, jobId, sort);

        model.addAllAttributes(data);
        addFilters(model, keyword, status, jobId, sort);

        return "recruiter/applications";
    }

    // =========================================
    // XEM CV THEO ĐƠN ỨNG TUYỂN
    // =========================================

    @GetMapping("/{applicationId}/cv")
    public String viewApplicationCv(
            @PathVariable Long applicationId,
            Model model) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        Map<String, Object> data =
                applicationService.getApplicationCv(applicationId, username);

        model.addAllAttributes(data);

        return "recruiter/cv-preview";
    }

    // =========================================
    // DUYỆT ĐƠN ỨNG TUYỂN
    // =========================================

    @PostMapping("/{applicationId}/approve")
    public String approveApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        Map<String, String> result =
                applicationService.approveApplication(applicationId, username);

        if ("/login".equals(result.get("redirect"))) {
            return "redirect:/login";
        }

        if (result.containsKey("successMessage")) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", result.get("successMessage"));
        }

        if (result.containsKey("errorMessage")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", result.get("errorMessage"));
        }

        return "redirect:" + result.get("redirect");
    }

    // =========================================
    // TỪ CHỐI ĐƠN ỨNG TUYỂN
    // =========================================

    @PostMapping("/{applicationId}/reject")
    public String rejectApplication(
            @PathVariable Long applicationId,
            RedirectAttributes redirectAttributes) {

        String username = getCurrentUsername();

        if (username == null) {
            return "redirect:/login";
        }

        Map<String, String> result =
                applicationService.rejectApplication(applicationId, username);

        if ("/login".equals(result.get("redirect"))) {
            return "redirect:/login";
        }

        if (result.containsKey("successMessage")) {
            redirectAttributes.addFlashAttribute(
                    "successMessage", result.get("successMessage"));
        }

        if (result.containsKey("errorMessage")) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", result.get("errorMessage"));
        }

        return "redirect:" + result.get("redirect");
    }

    // =========================================
    // HÀM HỖ TRỢ
    // =========================================

    private void addFilters(
            Model model,
            String keyword,
            String status,
            Long jobId,
            String sort) {

        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("jobId", jobId);
        model.addAttribute("sort", sort);
    }

    private String getCurrentUsername() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getName())) {
            return null;
        }

        return auth.getName();
    }
}