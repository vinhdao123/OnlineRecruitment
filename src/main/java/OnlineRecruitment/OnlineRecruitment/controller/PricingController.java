package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.config.BankPaymentConfig;
import OnlineRecruitment.OnlineRecruitment.entity.Payment;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.service.*;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

@Controller
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {
    private final ServicePackageService packageService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final BankPaymentConfig bank;
    private final OnlineRecruitment.OnlineRecruitment.config.SePayGatewayConfig gateway;
    private final SePayCheckoutService sePayCheckout;
    private final UserPackageRepository userPackageRepository;

    private User user(UserDetails principal) { return userService.getByEmail(principal.getUsername()); }

    @GetMapping
    public String pricingPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = user(principal);
        model.addAttribute("packages", packageService.getPackagesForRole(user.getRole()));
        model.addAttribute("currentUser", user);
        model.addAttribute("paymentReady", gateway.isReady() || bank.isReady());
        model.addAttribute("gatewaySandbox", gateway.isReady() && gateway.isSandbox());
        return "pricing/index";
    }

    @GetMapping("/checkout/{id}")
    public String legacyCheckout(@PathVariable Long id) { return "redirect:/pricing"; }

    @PostMapping("/checkout/{id}")
    public String checkout(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes ra) {
        if (!gateway.isReady() && !bank.isReady()) {
            ra.addFlashAttribute("error", "Thanh toán đang được thiết lập. Vui lòng quay lại sau.");
            return "redirect:/pricing";
        }
        Payment payment = gateway.isReady()
                ? paymentService.createGatewayPayment(user(principal), id, gateway.paymentMethod())
                : paymentService.createPendingPayment(user(principal), id);
        return "redirect:/pricing/resume/" + payment.getId();
    }

    @GetMapping("/resume/{id}")
    public String resumePayment(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, Model model) {
        Payment payment = paymentService.getOwnedPayment(id, user(principal).getId());
        model.addAttribute("payment", payment);

        // LUÔN TRẢ VỀ TRANG MỚI (gateway-checkout.html)
        model.addAttribute("sandbox", false);
        boolean canCheckout = gateway.isReady() && "PENDING".equals(payment.getStatus());
        model.addAttribute("canCheckout", canCheckout);
        if (canCheckout) {
            model.addAttribute("gatewayUrl", gateway.checkoutUrl());
            model.addAttribute("gatewayFields", sePayCheckout.form(payment));
        }
        return "pricing/gateway-checkout";
    }

    @GetMapping("/status/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Object>> status(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        Payment payment = paymentService.getOwnedPayment(id, user(principal).getId());
        return org.springframework.http.ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("id", payment.getId(), "status", payment.getStatus(), "paid", "SUCCESS".equals(payment.getStatus())));
    }

    @PostMapping("/confirm/{id}")
    public String confirmPayment(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes ra) {
        paymentService.userRequestConfirm(id, user(principal).getId());
        ra.addFlashAttribute("success", "Đã gửi yêu cầu đối soát. Gói chỉ kích hoạt khi giao dịch được xác nhận.");
        return "redirect:/pricing/resume/" + id;
    }

    @PostMapping("/cancel/{id}")
    public String cancelPayment(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal, RedirectAttributes ra) {
        paymentService.cancelPayment(id, user(principal).getId());
        ra.addFlashAttribute("success", "Đã hủy hóa đơn.");
        return "redirect:/pricing/history";
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal UserDetails principal, Model model) {
        User u = user(principal);
        model.addAttribute("payments", paymentService.getHistoryByUser(u));
        model.addAttribute("currentUserPackage",
                userPackageRepository.findFirstByUserAndStatusOrderByEndDateDesc(u, "ACTIVE").orElse(null));
        return "pricing/history";
    }
}
