package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CvTemplateVersionRepository extends JpaRepository<CvTemplateVersion, Long> {
    @EntityGraph(attributePaths = {"template"})
    List<CvTemplateVersion> findByIsPublishedTrue();

    @EntityGraph(attributePaths = {"template"})
    Optional<CvTemplateVersion> findWithTemplateById(Long id);
}
