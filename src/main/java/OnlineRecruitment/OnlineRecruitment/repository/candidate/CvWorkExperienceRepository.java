package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvWorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvWorkExperienceRepository extends JpaRepository<CvWorkExperience, Long> {
    List<CvWorkExperience> findByCvIdOrderByDisplayOrderAsc(Long cvId);

    // Tìm max display_order để xếp cuối
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(MAX(w.displayOrder), 0) FROM CvWorkExperience w WHERE w.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@org.springframework.data.repository.query.Param("cvId") Long cvId);
}