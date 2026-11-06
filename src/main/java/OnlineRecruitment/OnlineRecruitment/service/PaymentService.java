package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.PaymentRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final UserPackageRepository userPackageRepository;
    private final ServicePackageService packageService;

    @Transactional
    public Payment createPendingPayment(User user, Long packageId) {
        ServicePackage pkg = packageService.getById(packageId);
        if (!Boolean.TRUE.equals(pkg.getIsActive()) || pkg.getTargetRole() != user.getRole())
            throw new IllegalArgumentException("Gói dịch vụ không dành cho tài khoản này hoặc đã ngừng bán.");
        if (pkg.getPrice() == null || !Double.isFinite(pkg.getPrice()) || pkg.getPrice() <= 0
                || pkg.getPrice() % 1 != 0 || pkg.getDurationDays() == null || pkg.getDurationDays() < 1)
            throw new IllegalArgumentException("Giá hoặc thời hạn gói chưa hợp lệ.");
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setServicePackage(pkg);
        payment.setAmount(pkg.getPrice());
        payment.setStatus("PENDING");
        payment.setPaymentMethod("BANK_TRANSFER");
        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
    }

    @Transactional
    public Payment createGatewayPayment(User user, Long packageId, String method) {
        if (!List.of("SEPAY_SANDBOX", "SEPAY_LIVE").contains(method)) throw new IllegalArgumentException("Cổng thanh toán không hợp lệ.");
        Payment payment = createPendingPayment(user, packageId);
        payment.setPaymentMethod(method);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void receiveGatewayPayment(Long id, String transactionId, BigDecimal amount, String method) {
        Payment payment = lock(id);
        if (!method.equals(payment.getPaymentMethod())) throw new IllegalArgumentException("Môi trường hoặc cổng thanh toán không khớp hóa đơn.");
        if (amount.compareTo(BigDecimal.valueOf(payment.getAmount())) != 0)
            throw new IllegalArgumentException("Số tiền thanh toán không khớp hóa đơn.");
        String code = "PG_" + transactionId;
        if ("SUCCESS".equals(payment.getStatus())) {
            if (!code.equals(payment.getTransactionCode())) throw new IllegalArgumentException("Hóa đơn đã được thanh toán bằng giao dịch khác.");
            return;
        }
        if (paymentRepository.findByTransactionCode(code).isPresent())
            throw new IllegalArgumentException("Giao dịch đã được sử dụng.");
        if (!"PENDING".equals(payment.getStatus())) throw new IllegalArgumentException("Hóa đơn không còn chờ thanh toán; cần đối soát.");
        activate(payment, code);
    }

    public Payment getOwnedPayment(Long id, Long userId) {
        Payment payment = getPaymentById(id);
        checkOwner(payment, userId);
        return payment;
    }

    private void checkOwner(Payment payment, Long userId) {
        if (userId == null || !userId.equals(payment.getUser().getId()))
            throw new AccessDeniedException("Bạn không có quyền truy cập hóa đơn này.");
    }

    private Payment lock(Long id) {
        return paymentRepository.findLockedById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
    }

    public List<Payment> getHistoryByUser(User user) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void cancelPayment(Long id, Long userId) {
        Payment payment = lock(id);
        checkOwner(payment, userId);
        if (!"BANK_TRANSFER".equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Vui lòng quản lý giao dịch trên trang thanh toán SePay.");
        if (!"PENDING".equals(payment.getStatus()))
            throw new IllegalArgumentException("Chỉ được hủy hóa đơn chưa thanh toán.");
        payment.setStatus("FAILED");
        paymentRepository.save(payment);
    }

    @Transactional
    public void userRequestConfirm(Long id, Long userId) {
        Payment payment = lock(id);
        checkOwner(payment, userId);
        if (!"BANK_TRANSFER".equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Hóa đơn qua cổng SePay được xác nhận tự động bằng IPN.");
        if (!"PENDING".equals(payment.getStatus()))
            throw new IllegalArgumentException("Hóa đơn không còn chờ thanh toán.");
        payment.setStatus("WAITING_CONFIRM");
        paymentRepository.save(payment);
    }

    /** A database row lock serializes retries and manual approvals on one invoice. */
    @Transactional
    public void receiveBankTransfer(Long id, Long eventId, BigDecimal receivedAmount) {
        Payment payment = lock(id);
        if (!"BANK_TRANSFER".equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Hóa đơn không thuộc luồng webhook ngân hàng.");
        String code = "SEPAY_" + eventId;
        if ("SUCCESS".equals(payment.getStatus())) return;
        if (paymentRepository.findByTransactionCode(code).isPresent()) return;
        if (receivedAmount.compareTo(BigDecimal.valueOf(payment.getAmount())) != 0)
            throw new IllegalArgumentException("Số tiền chuyển khoản không khớp hóa đơn.");
        if (!List.of("PENDING", "WAITING_CONFIRM").contains(payment.getStatus()))
            throw new IllegalArgumentException("Hóa đơn đã hủy; cần đối soát giao dịch thủ công.");
        activate(payment, code);
    }

    @Transactional
    public void approvePayment(Long id) {
        Payment payment = lock(id);
        if ("SUCCESS".equals(payment.getStatus())) return;
        if (!"WAITING_CONFIRM".equals(payment.getStatus()))
            throw new IllegalArgumentException("Chỉ duyệt hóa đơn đang chờ xác nhận chuyển khoản.");
        activate(payment, "ADMIN_" + id);
    }

    private void activate(Payment payment, String transactionCode) {
        Integer days = payment.getServicePackage().getDurationDays();
        if (days == null || days < 1) throw new IllegalArgumentException("Thời hạn gói không hợp lệ.");
        payment.setStatus("SUCCESS");
        payment.setTransactionCode(transactionCode);
        paymentRepository.save(payment);
        UserPackage subscription = new UserPackage();
        subscription.setUser(payment.getUser());
        subscription.setServicePackage(payment.getServicePackage());
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(subscription.getStartDate().plusDays(days));
        subscription.setStatus("ACTIVE");
        userPackageRepository.save(subscription);
    }
}
