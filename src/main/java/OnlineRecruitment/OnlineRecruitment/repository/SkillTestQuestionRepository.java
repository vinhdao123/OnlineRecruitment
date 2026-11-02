package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillTestQuestionRepository extends JpaRepository<SkillTestQuestion, Long> {

    List<SkillTestQuestion> findBySkillTestIdAndStatus(Long skillTestId, String status);

    List<SkillTestQuestion> findBySkillTestId(Long skillTestId);

    long countBySkillTestId(Long skillTestId);

    @Query("SELECT q FROM SkillTestQuestion q WHERE q.skillTest.id = :testId AND q.status = 'ACTIVE'")
    List<SkillTestQuestion> findActivePoolByTestId(@Param("testId") Long testId);
}
