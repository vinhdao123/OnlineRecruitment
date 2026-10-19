package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    List<ServicePackage> findByTargetRoleAndIsActiveTrueOrderByPriceAsc(UserRole targetRole);
}