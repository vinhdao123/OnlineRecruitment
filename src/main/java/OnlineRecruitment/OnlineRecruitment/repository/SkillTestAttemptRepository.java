package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.SkillTestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillTestAttemptRepository extends JpaRepository<SkillTestAttempt, Long> {

    List<SkillTestAttempt> findByUserIdOrderByCompletedAtDesc(Long userId);

    Page<SkillTestAttempt> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);

    @Query("SELECT AVG(a.score) FROM SkillTestAttempt a WHERE a.user.id = :userId")
    Double getAverageScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM SkillTestAttempt a WHERE a.user.id = :userId AND a.isPassed = true")
    long countPassedByUserId(@Param("userId") Long userId);
}
