package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Payment;
import OnlineRecruitment.OnlineRecruitment.repository.PaymentRepository;
import OnlineRecruitment.OnlineRecruitment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @GetMapping
    public String listPayments(Model model) {
        List<Payment> payments = paymentRepository.findAllByOrderByCreatedAtDesc();

        // Tính tổng doanh thu (chỉ tính các hóa đơn SUCCESS)
        double totalRevenue = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .mapToDouble(Payment::getAmount)
                .sum();

        model.addAttribute("payments", payments);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("activeMenu", "payments");
        return "admin/payments/list";
    }

    // Duyệt thủ công hóa đơn (Dành cho trường hợp khách chuyển khoản tay)
    @PostMapping("/{id}/approve")
    public String approvePayment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            // Gọi hàm approvePayment mới
            paymentService.approvePayment(id);
            ra.addFlashAttribute("success", "Đã duyệt hóa đơn #" + id + " và kích hoạt gói thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/payments";
    }
}