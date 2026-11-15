package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.service.CandidateApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// =====================================================
// CONTROLLER ĐƠN ỨNG TUYỂN CỦA ỨNG VIÊN
//
// Controller chỉ lấy tài khoản đăng nhập, gọi
// CandidateApplicationService và đưa kết quả ra giao diện.
// Logic nằm ở service, SQL nằm ở CandidateApplicationRepository.
// =====================================================

@Controller
public class CandidateApplicationController {

    private final CandidateApplicationService candidateApplicationService;

    public CandidateApplicationController(
            CandidateApplicationService candidateApplicationService) {
        this.candidateApplicationService = candidateApplicationService;
    }

    // Lấy tài khoản đang đăng nhập
    private String getLoginName() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }

    // ==========================================
    // LẤY DANH SÁCH ĐƠN ỨNG TUYỂN
    //
    // Luồng:
    // 1. Chưa đăng nhập -> về /login
    // 2. Gọi service lấy danh sách đơn (kèm lịch phỏng vấn)
    // 3. Đưa lên trang candidate/my-applications
    // ==========================================

    @GetMapping("/candidate/applications")
    public String myApplications(Model model) {

        String loginName = getLoginName();

        if (loginName == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "applications",
                candidateApplicationService.getMyApplications(loginName)
        );

        return "candidate/my-applications";
    }

    // ==========================================
    // NỘP LẠI ĐƠN ỨNG TUYỂN
    //
    // Luồng:
    // 1. Chưa đăng nhập -> về /login
    // 2. Gọi service nộp lại đơn (service kiểm tra toàn bộ điều kiện)
    // 3. Thành công -> successMessage
    //    Lỗi nghiệp vụ -> errorMessage lấy từ exception
    // 4. Luôn quay về trang danh sách đơn
    // ==========================================

    @PostMapping("/candidate/apply-again/{jobId}")
    public String applyAgain(
            @PathVariable("jobId") int jobId,
            RedirectAttributes redirectAttributes) {

        String loginName = getLoginName();

        if (loginName == null) {
            return "redirect:/login";
        }

        try {
            candidateApplicationService.applyAgain(loginName, jobId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Nộp lại đơn xin việc thành công!"
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/candidate/applications";
    }
}
