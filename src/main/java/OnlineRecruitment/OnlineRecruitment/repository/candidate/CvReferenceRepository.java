package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvReferenceRepository extends JpaRepository<CvReference, Long> {
    List<CvReference> findByCvIdOrderByDisplayOrderAsc(Long cvId);

    @Query("SELECT COALESCE(MAX(r.displayOrder), 0) FROM CvReference r WHERE r.cvId = :cvId")
    Integer findMaxDisplayOrderByCvId(@Param("cvId") Long cvId);
}