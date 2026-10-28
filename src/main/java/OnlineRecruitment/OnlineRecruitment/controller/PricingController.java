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

import java.util.*;

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

    @GetMapping("/checkout/{id}")
    public String checkout(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails principal,
                           Model model) {
        User user = userService.getByEmail(principal.getUsername());
        Payment payment = paymentService.createPendingPayment(user, id);
        model.addAttribute("payment", payment);
        return "pricing/checkout";
    }

    // Hàm này dùng để User bấm nút xác nhận (nếu bạn vẫn giữ giao diện cũ)
    @PostMapping("/confirm/{paymentId}")
    public String confirmPayment(@PathVariable Long paymentId, RedirectAttributes ra) {
        try {
            paymentService.confirmPayment(paymentId); // Đã sửa thành confirmPayment để kích hoạt luôn
            ra.addFlashAttribute("success", "Thanh toán thành công! Gói đã được kích hoạt.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pricing/history";
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.getByEmail(principal.getUsername());
        model.addAttribute("payments", paymentService.getHistoryByUser(user));
        return "pricing/history";
    }

    @GetMapping("/resume/{paymentId}")
    public String resumePayment(@PathVariable Long paymentId, Model model) {
        Payment payment = paymentService.getPaymentById(paymentId);
        if (!"PENDING".equals(payment.getStatus())) {
            return "redirect:/pricing/history";
        }
        model.addAttribute("payment", payment);
        return "pricing/checkout";
    }

    @PostMapping("/cancel/{paymentId}")
    public String cancelPayment(@PathVariable Long paymentId, RedirectAttributes ra) {
        paymentService.cancelPayment(paymentId);
        ra.addFlashAttribute("success", "Đã hủy hóa đơn chờ thanh toán.");
        return "redirect:/pricing/history";
    }

    // ==========================================
    // HÀM API MỚI: DÀNH CHO JAVASCRIPT Ở TRANG CHECKOUT
    // (Hàm này giúp trình duyệt tự động kiểm tra xem gói đã được kích hoạt chưa)
    // ==========================================
    @GetMapping("/api/payment-status/{paymentId}")
    @ResponseBody
    public Map<String, String> checkPaymentStatus(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPaymentById(paymentId);
        return Map.of("status", payment.getStatus());
    }

    @GetMapping("/success")
    public String paymentSuccess() {
        return "pricing/success"; // Trả về file success.html
    }

}