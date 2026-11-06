package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.Payment;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.service.PaymentService;
import OnlineRecruitment.OnlineRecruitment.service.ServicePackageService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final ServicePackageService packageService;
    private final UserService userService;
    private final PaymentService paymentService;

    @GetMapping
    public String pricingPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        model.addAttribute("packages", packageService.getPackagesForRole(user.getRole()));
        model.addAttribute("currentUser", user);
        return "pricing/index";
    }

    // Chuyển hướng sang trang Thanh toán
    @GetMapping("/checkout/{id}")
    public String checkout(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails principal,
                           Model model) {
        User user = userService.getByEmail(principal.getUsername());
        Payment payment = paymentService.createPendingPayment(user, id);

        model.addAttribute("payment", payment);
        return "pricing/checkout";
    }

    // Xử lý khi User bấm nút "Tôi đã chuyển khoản"
    @PostMapping("/confirm/{paymentId}")
    public String confirmPayment(@PathVariable Long paymentId, RedirectAttributes ra) {
        try {
            // Gọi hàm userRequestConfirm thay vì confirmPayment cũ
            paymentService.userRequestConfirm(paymentId);
            ra.addFlashAttribute("success", "Đã ghi nhận. Vui lòng chờ Admin kiểm tra và duyệt giao dịch!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pricing/history";
    }

    // Xem lịch sử giao dịch
    @GetMapping("/history")
    public String history(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        model.addAttribute("payments", paymentService.getHistoryByUser(user));
        return "pricing/history";
    }

    // 1. Thanh toán tiếp hóa đơn cũ
    @GetMapping("/resume/{paymentId}")
    public String resumePayment(@PathVariable Long paymentId, Model model) {
        Payment payment = paymentService.getPaymentById(paymentId);

        // Chỉ cho phép thanh toán tiếp nếu đang PENDING
        if (!"PENDING".equals(payment.getStatus())) {
            return "redirect:/pricing/history";
        }

        model.addAttribute("payment", payment);
        return "pricing/checkout"; // Trả về lại trang QR cũ
    }

    // 2. Hủy hóa đơn
    @PostMapping("/cancel/{paymentId}")
    public String cancelPayment(@PathVariable Long paymentId, RedirectAttributes ra) {
        paymentService.cancelPayment(paymentId);
        ra.addFlashAttribute("success", "Đã hủy hóa đơn chờ thanh toán.");
        return "redirect:/pricing/history";
    }



}