package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvEducationRepository extends JpaRepository<CvEducation, Long> {
    List<CvEducation> findByCvIdOrderByDisplayOrderAsc(Long cvId);

    @Query("SELECT COALESCE(MAX(e.displayOrder), 0) FROM CvEducation e WHERE e.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@Param("cvId") Long cvId);
}