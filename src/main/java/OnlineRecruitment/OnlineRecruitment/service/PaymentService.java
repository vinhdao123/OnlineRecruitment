package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.PaymentRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserPackageRepository userPackageRepository;
    private final ServicePackageService packageService;

    // 1. Tạo Hóa đơn PENDING khi user bấm Mua
    public Payment createPendingPayment(User user, Long packageId) {
        ServicePackage pkg = packageService.getById(packageId);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setServicePackage(pkg);
        payment.setAmount(pkg.getPrice());
        payment.setStatus("PENDING");
        payment.setPaymentMethod("BANK_TRANSFER");

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn"));
    }

    // 2. Xử lý khi user xác nhận đã chuyển khoản
    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn này đã được thanh toán rồi!");
        }

        // Cập nhật trạng thái Payment
        payment.setStatus("SUCCESS");
        payment.setTransactionCode("FT" + System.currentTimeMillis()); // Giả lập mã giao dịch
        paymentRepository.save(payment);

        // Kích hoạt gói cho User (UserPackage)
        UserPackage userPackage = new UserPackage();
        userPackage.setUser(payment.getUser());
        userPackage.setServicePackage(payment.getServicePackage());
        userPackage.setStartDate(LocalDateTime.now());
        userPackage.setEndDate(LocalDateTime.now().plusDays(payment.getServicePackage().getDurationDays()));
        userPackage.setStatus("ACTIVE");

        userPackageRepository.save(userPackage);
    }

    public List<Payment> getHistoryByUser(User user) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // Hủy hóa đơn
    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if ("PENDING".equals(payment.getStatus())) {
            payment.setStatus("FAILED"); // Đổi thành thất bại/hủy
            paymentRepository.save(payment);
        }
    }

    // Hàm mới: Dành cho User bấm "Tôi đã chuyển khoản"
    @Transactional
    public void userRequestConfirm(Long paymentId) {
        Payment payment = getPaymentById(paymentId);
        if ("PENDING".equals(payment.getStatus())) {
            payment.setStatus("WAITING_CONFIRM"); // Đổi thành chờ duyệt
            paymentRepository.save(payment);
        }
    }

    // Hàm cũ: Sửa lại thành hàm Duyệt (Chỉ Admin gọi)
    @Transactional
    public void approvePayment(Long paymentId) {
        Payment payment = getPaymentById(paymentId);

        if ("SUCCESS".equals(payment.getStatus())) {
            throw new IllegalArgumentException("Hóa đơn này đã được duyệt rồi!");
        }

        // Cập nhật trạng thái Payment
        payment.setStatus("SUCCESS");
        payment.setTransactionCode("ADMIN_APPROVED_" + System.currentTimeMillis());
        paymentRepository.save(payment);

        // Kích hoạt gói cho User
        UserPackage userPackage = new UserPackage();
        userPackage.setUser(payment.getUser());
        userPackage.setServicePackage(payment.getServicePackage());
        userPackage.setStartDate(LocalDateTime.now());
        userPackage.setEndDate(LocalDateTime.now().plusDays(payment.getServicePackage().getDurationDays()));
        userPackage.setStatus("ACTIVE");

        userPackageRepository.save(userPackage);
    }

}