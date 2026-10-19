package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvProjectRepository extends JpaRepository<CvProject, Long> {
    List<CvProject> findByCvIdOrderByDisplayOrderAsc(Long cvId);

    @Query("SELECT COALESCE(MAX(p.displayOrder), 0) FROM CvProject p WHERE p.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@Param("cvId") Long cvId);
}