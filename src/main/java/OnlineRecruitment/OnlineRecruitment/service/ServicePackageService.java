package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.ServicePackageRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServicePackageService {

    private final ServicePackageRepository packageRepository;
    private final UserPackageRepository userPackageRepository;
    private final JobRepository jobRepository;

    public List<ServicePackage> getAllPackages() {
        return packageRepository.findAll();
    }

    public ServicePackage getById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói dịch vụ"));
    }

    public ServicePackage save(ServicePackage pkg) {
        if (pkg.getPrice() == null || pkg.getPrice() <= 0)
            throw new IllegalArgumentException("Giá gói phải lớn hơn 0.");

        // SỬA Ở ĐÂY: Chỉ check thời hạn nếu là gói MAIN
        if ("MAIN".equals(pkg.getPackageType())) {
            if (pkg.getDurationDays() == null || pkg.getDurationDays() < 1) {
                throw new IllegalArgumentException("Thời hạn phải ít nhất 1 ngày.");
            }
        }

        if (pkg.getIsActive() == null)
            pkg.setIsActive(true);
        return packageRepository.save(pkg);
    }

    public void toggleStatus(Long id) {
        ServicePackage pkg = getById(id);
        pkg.setIsActive(!pkg.getIsActive());
        packageRepository.save(pkg);
    }

    public List<ServicePackage> getPackagesForRole(UserRole role) {
        return packageRepository.findByTargetRoleAndIsActiveTrueOrderByPriceAsc(role);
    }

    @Transactional
    public UserPackage getActivePackage(User user) {
        Optional<UserPackage> opt = userPackageRepository
                .findFirstByUserAndStatusOrderByEndDateDesc(user, "ACTIVE");
        if (opt.isEmpty()) return null;

        UserPackage pkg = opt.get();
        if (pkg.getEndDate().isBefore(LocalDateTime.now())) {
            pkg.setStatus("EXPIRED");
            userPackageRepository.save(pkg);
            return null;
        }
        return pkg;
    }

    // ===== SỬA LẠI HÀM NÀY =====
    public int getRemainingJobs(User user) {
        UserPackage activePkg = getActivePackage(user);

        // 1. Nếu chưa có gói ACTIVE
        if (activePkg == null) {
            // Đếm số bài đã đăng (tất cả trạng thái) của user
            long usedCount = jobRepository.countByRecruiter_Id(user.getId());
            // Chỉ được đăng 1 bài miễn phí
            return (int) Math.max(0, 1 - usedCount);
        }

        // 2. Nếu có gói ACTIVE
        Integer max = activePkg.getSnapshotMaxJobs();
        if (max == null) return -1; // -1 tượng trưng cho vô hạn

        int used = activePkg.getUsedJobs() == null ? 0 : activePkg.getUsedJobs();
        return Math.max(0, max - used);
    }
    // ===========================

    public String getQuotaLabel(User user) {
        UserPackage activePkg = getActivePackage(user);
        if (activePkg == null) {
            long usedCount = jobRepository.countByRecruiter_Id(user.getId());
            long remaining = Math.max(0, 1 - usedCount);
            return "Chưa có gói (còn " + remaining + "/1 bài miễn phí)";
        }
        Integer max = activePkg.getSnapshotMaxJobs();
        if (max == null) return "Không giới hạn số tin";
        int used = activePkg.getUsedJobs() == null ? 0 : activePkg.getUsedJobs();
        return "Còn " + (max - used) + "/" + max + " tin";
    }

    public boolean canPinJob(User user) {
        UserPackage activePkg = getActivePackage(user);
        return activePkg != null && Boolean.TRUE.equals(activePkg.getAllowPinJob());
    }

    // Kiểm tra user có được phép mua gói này không
    public String checkCanBuy(User user, ServicePackage pkg) {
        UserPackage activePkg = getActivePackage(user);

        // Nếu là TOPUP, bắt buộc phải có gói chính
        if ("TOPUP".equals(pkg.getPackageType())) {
            if (activePkg == null) {
                return "Cần mua gói chính trước khi mua thêm tin.";
            }
            return null; // OK
        }

        // Nếu là MAIN, kiểm tra xem có đang dùng gói khác không
        if (activePkg != null) {
            if (!activePkg.getServicePackage().getId().equals(pkg.getId())) {
                return "Bạn đang dùng gói " + activePkg.getServicePackage().getName() + ". Không thể đổi gói khi chưa hết hạn.";
            }
        }
        return null; // OK
    }

}