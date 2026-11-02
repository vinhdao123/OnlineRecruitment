package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.Payment;
import OnlineRecruitment.OnlineRecruitment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SepayWebhookController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/sepay-webhook")
    public ResponseEntity<?> handleSepayWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // 1. In ra log để xem SePay gửi gì về
            System.out.println("=== NHẬN WEBHOOK TỪ SEPAY ===");
            System.out.println("Dữ liệu: " + payload);

            // 2. Lấy nội dung chuyển khoản (VD: "MUA GOI 10043")
            String content = (String) payload.get("content");
            if (content == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không có nội dung"));
            }

            // 3. Trích xuất mã đơn hàng (chỉ lấy phần số)
            String paymentIdStr = content.replaceAll("\\D+", "");
            if (paymentIdStr.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", false, "message", "Không tìm thấy mã đơn"));
            }

            Long paymentId = Long.parseLong(paymentIdStr);
            Payment payment = paymentService.getPaymentById(paymentId);

            // 4. (Bảo mật) Kiểm tra số tiền chuyển vào có khớp với hóa đơn không
            Number transferAmount = (Number) payload.get("transferAmount");
            if (transferAmount != null && payment.getAmount().longValue() != transferAmount.longValue()) {
                System.out.println("CẢNH BÁO: Số tiền không khớp! Hóa đơn: " + payment.getAmount() + ", Nhận được: " + transferAmount);
                return ResponseEntity.ok(Map.of("success", false, "message", "Số tiền không khớp"));
            }

            // 5. Tự động kích hoạt gói
            paymentService.confirmPayment(paymentId);
            System.out.println("=> Đã kích hoạt gói thành công cho hóa đơn #" + paymentId);

            // 6. Phản hồi cho SePay biết đã nhận thành công
            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            System.out.println("Lỗi xử lý webhook: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}