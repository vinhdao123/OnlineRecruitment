package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
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

    public List<ServicePackage> getAllPackages() {
        return packageRepository.findAll();
    }

    public ServicePackage getById(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói dịch vụ"));
    }

    /**
     * Lưu gói. Validate cơ bản để tránh lưu dữ liệu sai nếu gọi trực tiếp.
     */
    public ServicePackage save(ServicePackage pkg) {
        if (pkg.getPrice() == null || pkg.getPrice() <= 0)
            throw new IllegalArgumentException("Giá gói phải lớn hơn 0.");
        if (pkg.getDurationDays() == null || pkg.getDurationDays() < 1)
            throw new IllegalArgumentException("Thời hạn phải ít nhất 1 ngày.");
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

    /**
     * Lấy gói ACTIVE hiện tại của user.
     * Tự động chuyển sang EXPIRED nếu đã quá endDate (lazy expire).
     */
    @Transactional
    public UserPackage getActivePackage(User user) {
        Optional<UserPackage> opt = userPackageRepository
                .findFirstByUserAndStatusOrderByEndDateDesc(user, "ACTIVE");
        if (opt.isEmpty()) return null;

        UserPackage pkg = opt.get();
        if (pkg.getEndDate().isBefore(LocalDateTime.now())) {
            pkg.setStatus("EXPIRED");
            userPackageRepository.save(pkg);
            return null;   // Coi như chưa có gói
        }
        return pkg;
    }
}