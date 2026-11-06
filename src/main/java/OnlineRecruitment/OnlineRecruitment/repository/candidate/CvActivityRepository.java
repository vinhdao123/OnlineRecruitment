package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvActivityRepository extends JpaRepository<CvActivity, Long> {
    List<CvActivity> findByCvIdOrderByDisplayOrderAsc(Long cvId);

    @Query("SELECT COALESCE(MAX(a.displayOrder), 0) FROM CvActivity a WHERE a.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@Param("cvId") Long cvId);
}