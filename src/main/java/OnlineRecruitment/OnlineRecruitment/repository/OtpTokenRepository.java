package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findTopByEmailOrderByCreatedAtDesc(String email);

    boolean existsByUserIdAndUsedAtIsNotNull(Long userId);

    boolean existsByEmailAndUsedAtIsNotNull(String email);
}