package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    List<Cv> findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status);

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    List<Cv> findByCandidateIdAndIdIn(Long candidateId, Collection<Long> ids);

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    Page<Cv> findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status, Pageable pageable);

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    Page<Cv> findByCandidateIdAndStatusAndTitleContainingIgnoreCaseOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status, String title, Pageable pageable);

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    Optional<Cv> findByIdAndCandidateId(Long id, Long candidateId);

    @EntityGraph(attributePaths = {"templateVersion", "templateVersion.template"})
    Optional<Cv> findWithTemplateById(Long id);

    Optional<Cv> findByCandidateIdAndIsDefaultTrueAndStatus(Long candidateId, String status);
}
