package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.service.ApplicationSubmitService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.JacksonException;

// =====================================================
// CONTROLLER NỘP HỒ SƠ ỨNG TUYỂN
//
// Luồng:
// 1. Kiểm tra đăng nhập -> chưa đăng nhập thì về /login
// 2. Gọi ApplicationSubmitService xử lý toàn bộ nghiệp vụ nộp đơn
// 3. Chuyển kết quả / lỗi thành thông báo flash
// 4. Quay lại trang ứng tuyển của công việc
// =====================================================

@Controller
public class ApplicationSubmitController {

    private final ApplicationSubmitService applicationSubmitService;

    public ApplicationSubmitController(ApplicationSubmitService applicationSubmitService) {
        this.applicationSubmitService = applicationSubmitService;
    }

    @PostMapping("/applications/submit")
    public String submitApplication(
            @RequestParam Long jobId,
            @RequestParam Long cvId,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) Boolean confirmInformation,
            RedirectAttributes redirectAttributes) {

        String redirectUrl = "redirect:/jobs/" + jobId + "/apply";

        // 1. Kiểm tra người dùng đang đăng nhập
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        try {
            // 2. Nộp đơn
            ApplicationSubmitService.SubmitResult result =
                    applicationSubmitService.submitApplication(
                            authentication.getName(), jobId, cvId);

            // 3. Gắn thông báo theo kết quả
            if (result == ApplicationSubmitService.SubmitResult.ALREADY_APPLIED) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Bạn đã có đơn ứng tuyển cho công việc này."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Gửi hồ sơ ứng tuyển thành công!"
                );
            }

        } catch (JacksonException e) {
            // Lỗi khi chuyển CV thành JSON snapshot
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Không thể tạo bản lưu CV. Vui lòng thử lại.");

        } catch (Exception e) {
            // Lỗi nghiệp vụ (không tìm thấy job / CV / ứng viên...) hoặc lỗi DB
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Gửi hồ sơ thất bại: " + e.getMessage());
        }

        // 4. Quay lại trang ứng tuyển
        return redirectUrl;
    }
}
