package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvTemplateVersionRepository extends JpaRepository<CvTemplateVersion, Long> {
    List<CvTemplateVersion> findByIsPublishedTrue();
}