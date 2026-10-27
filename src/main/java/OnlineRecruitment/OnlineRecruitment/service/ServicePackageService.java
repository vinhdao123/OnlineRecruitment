package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.repository.ServicePackageRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public ServicePackage save(ServicePackage pkg) {
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

    public UserPackage getActivePackage(User user) {
        return userPackageRepository.findFirstByUserAndStatusOrderByEndDateDesc(user, "ACTIVE")
                .orElse(null);
    }
}