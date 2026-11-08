package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {

    List<Cv> findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status);

    Page<Cv> findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status, Pageable pageable);

    Page<Cv> findByCandidateIdAndStatusAndTitleContainingIgnoreCaseOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status, String title, Pageable pageable);

    Optional<Cv> findByIdAndCandidateId(Long id, Long candidateId);
    Optional<Cv> findByCandidateIdAndIsDefaultTrueAndStatus(Long candidateId, String status);
}
