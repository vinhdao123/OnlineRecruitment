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

    // ==========================================
    // 1. TẠO HÓA ĐƠN
    // ==========================================

    @Transactional
    public Payment createPendingPayment(User user, Long packageId) {
        ServicePackage pkg = packageService.getById(packageId);
        if (!Boolean.TRUE.equals(pkg.getIsActive()) || pkg.getTargetRole() != user.getRole())
            throw new IllegalArgumentException("Gói dịch vụ không dành cho tài khoản này hoặc đã ngừng bán.");

        // 1. Validate Giá tiền (Áp dụng cho cả MAIN và TOPUP)
        if (pkg.getPrice() == null || !Double.isFinite(pkg.getPrice()) || pkg.getPrice() <= 0 || pkg.getPrice() % 1 != 0) {
            throw new IllegalArgumentException("Giá gói chưa hợp lệ.");
        }

        // 2. Validate Thời hạn (CHỈ áp dụng cho gói MAIN)
        String type = pkg.getPackageType() == null ? "MAIN" : pkg.getPackageType();
        if ("MAIN".equals(type)) {
            if (pkg.getDurationDays() == null || pkg.getDurationDays() < 1) {
                throw new IllegalArgumentException("Thời hạn gói chưa hợp lệ.");
            }
        }

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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
    }

    @Transactional
    public Payment createGatewayPayment(User user, Long packageId, String method) {
        if (!List.of("SEPAY_SANDBOX", "SEPAY_LIVE").contains(method))
            throw new IllegalArgumentException("Cổng thanh toán không hợp lệ.");
        Payment payment = createPendingPayment(user, packageId);
        payment.setPaymentMethod(method);
        return paymentRepository.save(payment);
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
        return paymentRepository.findLockedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
    }

    public List<Payment> getHistoryByUser(User user) {
        return paymentRepository.findByUserOrderByCreatedAtDesc(user);
    }

    // ==========================================
    // 2. NHẬN KẾT QUẢ THANH TOÁN
    // ==========================================

    /** Luồng cổng thanh toán SePay (SEPAY_SANDBOX / SEPAY_LIVE). */
    @Transactional
    public void receiveGatewayPayment(Long id, String transactionId, BigDecimal amount, String method) {
        Payment payment = lock(id);
        if (!method.equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Môi trường hoặc cổng thanh toán không khớp hóa đơn.");
        if (amount.compareTo(BigDecimal.valueOf(payment.getAmount())) != 0)
            throw new IllegalArgumentException("Số tiền thanh toán không khớp hóa đơn.");
        String code = "PG_" + transactionId;
        if ("SUCCESS".equals(payment.getStatus())) {
            if (!code.equals(payment.getTransactionCode()))
                throw new IllegalArgumentException("Hóa đơn đã được thanh toán bằng giao dịch khác.");
            return;
        }
        if (paymentRepository.findByTransactionCode(code).isPresent())
            throw new IllegalArgumentException("Giao dịch đã được sử dụng.");
        if (!"PENDING".equals(payment.getStatus()))
            throw new IllegalArgumentException("Hóa đơn không còn chờ thanh toán; cần đối soát.");
        activate(payment, code);
    }

    @Transactional
    public void receiveBankTransfer(Long id, Long eventId, BigDecimal receivedAmount) {
        // 1. Validate đầu vào
        if (eventId == null)
            throw new IllegalArgumentException("Thiếu mã sự kiện (eventId) từ webhook.");
        if (receivedAmount == null)
            throw new IllegalArgumentException("Thiếu số tiền nhận được từ webhook.");

        // 2. Khóa dòng hóa đơn
        Payment payment = lock(id);

        // 3. Chỉ chấp nhận hóa đơn thuộc luồng BANK_TRANSFER
        if (!"BANK_TRANSFER".equals(payment.getPaymentMethod()))
            throw new IllegalArgumentException("Hóa đơn không thuộc luồng webhook ngân hàng.");

        String code = "SEPAY_" + eventId;

        // 4. Idempotency: hóa đơn đã SUCCESS với đúng eventId này → bỏ qua
        if ("SUCCESS".equals(payment.getStatus())) {
            if (code.equals(payment.getTransactionCode())) return;          // retry bình thường
            throw new IllegalStateException("Hóa đơn đã thanh toán bằng giao dịch khác: "
                    + payment.getTransactionCode());
        }

        // 5. eventId đã dùng cho hóa đơn khác → KHÔNG nuốt lỗi, báo để đối soát
        if (paymentRepository.findByTransactionCode(code).isPresent())
            throw new IllegalStateException("Mã giao dịch " + code
                    + " đã được dùng cho hóa đơn khác; cần đối soát thủ công.");

        // 6. Kiểm tra số tiền
        if (receivedAmount.compareTo(BigDecimal.valueOf(payment.getAmount())) != 0)
            throw new IllegalArgumentException(
                    "Số tiền chuyển khoản không khớp hóa đơn (nhận "
                            + receivedAmount + ", cần " + payment.getAmount() + ").");

        // 7. Chỉ kích hoạt khi hóa đơn còn ở trạng thái chờ
        if (!List.of("PENDING", "WAITING_CONFIRM").contains(payment.getStatus()))
            throw new IllegalStateException("Hóa đơn đang ở trạng thái " + payment.getStatus()
                    + "; cần đối soát giao dịch thủ công trước khi kích hoạt.");

        // 8. Kích hoạt gói
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

    // ==========================================
    // 3. KÍCH HOẠT & GIA HẠN GÓI (CỘNG DỒN NGÀY)
    // ==========================================

    /**
     * Kích hoạt gói sau khi thanh toán thành công.
     * Được gọi chung từ 3 luồng: webhook ngân hàng, cổng SePay, admin duyệt tay.
     *
     * Quy tắc:
     *  - Cùng gói đang ACTIVE  → cộng dồn endDate (nếu đã quá hạn thì tính từ hôm nay)
     *  - Khác gói               → hủy gói cũ (CANCELLED) rồi tạo gói mới
     *  - Chưa có gói            → tạo mới
     */
    private void activate(Payment payment, String transactionCode) {
        ServicePackage newPkg = payment.getServicePackage();
        String type = newPkg.getPackageType() == null ? "MAIN" : newPkg.getPackageType();
        Integer newMaxJobs = newPkg.getMaxJobs();

        // 1. Chốt hóa đơn
        payment.setStatus("SUCCESS");
        payment.setTransactionCode(transactionCode);
        paymentRepository.save(payment);

        // 2. Tìm gói ACTIVE hiện tại
        UserPackage activePkg = userPackageRepository
                .findFirstByUserAndStatusOrderByEndDateDesc(payment.getUser(), "ACTIVE")
                .orElse(null);

        // ============================================================
        // TRƯỜNG HỢP 1: TOP-UP — chỉ cộng tin, KHÔNG đổi hạn/ghim
        // ============================================================
        if ("TOPUP".equals(type)) {
            if (activePkg == null) {
                throw new IllegalStateException("Bạn cần mua gói chính (Basic/Pro) trước khi mua thêm tin.");
            }
            if (newMaxJobs != null) {
                int oldMax = activePkg.getSnapshotMaxJobs() == null ? 0 : activePkg.getSnapshotMaxJobs();
                activePkg.setSnapshotMaxJobs(oldMax + newMaxJobs);
                userPackageRepository.save(activePkg);
            } else {
                activePkg.setSnapshotMaxJobs(null); // null = vô hạn
                userPackageRepository.save(activePkg);
            }
            return;
        }

        // ============================================================
        // TRƯỜNG HỢP 2: MUA GÓI CHÍNH (MAIN)
        // ============================================================
        Integer days = newPkg.getDurationDays();
        if (days == null || days < 1) throw new IllegalArgumentException("Thời hạn gói chính không hợp lệ.");

        Boolean newAllowPin = Boolean.TRUE.equals(newPkg.getAllowPinJob());

        if (activePkg != null) {
            boolean samePackage = activePkg.getServicePackage().getId().equals(newPkg.getId());

            if (!samePackage) {
                // 2a. Mua gói chính KHÁC -> CHẶN (không cho đổi gói khi chưa hết hạn)
                throw new IllegalStateException(
                        "Bạn đang dùng gói \"" + activePkg.getServicePackage().getName()
                                + "\". Không thể đổi sang gói khác khi gói hiện tại chưa hết hạn.");
            }

            // 2b. Mua lại ĐÚNG gói -> cộng dồn ngày + cộng dồn số tin
            LocalDateTime base = activePkg.getEndDate().isAfter(LocalDateTime.now())
                    ? activePkg.getEndDate() : LocalDateTime.now();
            activePkg.setEndDate(base.plusDays(days));

            if (newMaxJobs != null) {
                int oldMax = activePkg.getSnapshotMaxJobs() == null ? 0 : activePkg.getSnapshotMaxJobs();
                activePkg.setSnapshotMaxJobs(oldMax + newMaxJobs);
            } else {
                activePkg.setSnapshotMaxJobs(null); // Gói vô hạn
            }
            if (newAllowPin) activePkg.setAllowPinJob(true);

            userPackageRepository.save(activePkg);
            return;
        }

        // ============================================================
        // TRƯỜNG HỢP 3: MUA LẦN ĐẦU
        // ============================================================
        UserPackage newUserPkg = new UserPackage();
        newUserPkg.setUser(payment.getUser());
        newUserPkg.setServicePackage(newPkg);
        newUserPkg.setStartDate(LocalDateTime.now());
        newUserPkg.setEndDate(LocalDateTime.now().plusDays(days));
        newUserPkg.setStatus("ACTIVE");
        newUserPkg.setUsedJobs(0);
        newUserPkg.setSnapshotMaxJobs(newMaxJobs);
        newUserPkg.setAllowPinJob(newAllowPin);
        userPackageRepository.save(newUserPkg);
    }

    // ==========================================
    // 4. HỦY & YÊU CẦU XÁC NHẬN
    // ==========================================

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
}