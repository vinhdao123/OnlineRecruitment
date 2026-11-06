package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserPackageRepository extends JpaRepository<UserPackage, Long> {
    // Tìm gói đang ACTIVE của user
    Optional<UserPackage> findFirstByUserAndStatusOrderByEndDateDesc(User user, String status);
}