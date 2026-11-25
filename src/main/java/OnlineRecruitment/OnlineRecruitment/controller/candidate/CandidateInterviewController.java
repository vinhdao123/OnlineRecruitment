package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.service.CandidateInterviewService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

// =====================================================
// CONTROLLER ĐẶT LỊCH PHỎNG VẤN CỦA ỨNG VIÊN
//
// Controller chỉ kiểm tra đăng nhập, gọi CandidateInterviewService
// và đưa dữ liệu / thông báo ra giao diện.
// Logic nằm ở service, SQL nằm ở CandidateInterviewRepository.
// =====================================================

@Controller
public class CandidateInterviewController {

    private final CandidateInterviewService candidateInterviewService;

    public CandidateInterviewController(
            CandidateInterviewService candidateInterviewService
    ) {
        this.candidateInterviewService = candidateInterviewService;
    }

    // Kiểm tra người dùng đã đăng nhập (không phải anonymous)
    private boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    // =====================================================
    // 1. HIỂN THỊ KHUNG GIỜ PHỎNG VẤN
    //
    // Luồng:
    // 1. Chưa đăng nhập -> về /login
    // 2. Gọi service lấy đơn + khung giờ
    //    -> đơn không hợp lệ / chưa ACCEPTED / đã đặt lịch:
    //       quay về danh sách đơn
    // 3. Gửi dữ liệu sang Thymeleaf
    // =====================================================

    @GetMapping("/candidate/interviews/{applicationId}")
    public String showInterviewSlots(
            @PathVariable Long applicationId,
            Authentication authentication,
            Model model
    ) {

        // 1. Kiểm tra đăng nhập
        if (!isLoggedIn(authentication)) {
            return "redirect:/login";
        }

        // 2. Lấy dữ liệu trang đặt lịch
        Optional<CandidateInterviewService.BookingPage> page =
                candidateInterviewService.getBookingPage(
                        applicationId, authentication.getName());

        if (page.isEmpty()) {
            return "redirect:/candidate/applications";
        }

        // 3. Gửi dữ liệu sang Thymeleaf
        model.addAttribute("application", page.get().application());
        model.addAttribute("applicationId", applicationId);
        model.addAttribute("slots", page.get().slots());

        return "candidate/interview-booking";
    }

    // =====================================================
    // 2. XÁC NHẬN ĐẶT LỊCH PHỎNG VẤN
    //
    // Luồng:
    // 1. Chưa đăng nhập -> về /login
    // 2. Gọi service đặt lịch (service kiểm tra ứng viên, đơn, khung giờ)
    // 3. Thành công -> successMessage, lỗi -> errorMessage từ exception
    // 4. Quay về danh sách đơn
    // =====================================================

    @PostMapping("/candidate/interviews/book")
    public String bookInterview(
            @RequestParam Long applicationId,
            @RequestParam Long availabilityId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        // 1. Kiểm tra đăng nhập
        if (!isLoggedIn(authentication)) {
            return "redirect:/login";
        }

        // 2. Đặt lịch
        try {
            candidateInterviewService.bookInterview(
                    authentication.getName(), applicationId, availabilityId);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đặt lịch phỏng vấn thành công!"
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/candidate/applications";
    }
}
