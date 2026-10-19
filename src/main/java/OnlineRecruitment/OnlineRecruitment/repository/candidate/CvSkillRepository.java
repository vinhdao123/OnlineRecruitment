package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvSkill;
import OnlineRecruitment.OnlineRecruitment.entity.CvSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvSkillRepository extends JpaRepository<CvSkill, CvSkillId> {
    List<CvSkill> findByIdCvIdOrderByDisplayOrderAsc(Long cvId);

    @Query("SELECT COALESCE(MAX(cs.displayOrder), 0) FROM CvSkill cs WHERE cs.id.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@Param("cvId") Long cvId);
}