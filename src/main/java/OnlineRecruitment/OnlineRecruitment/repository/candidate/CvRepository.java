package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {

    List<Cv> findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
            Long candidateId, String status);

    java.util.Optional<Cv> findByIdAndCandidateId(Long id, Long candidateId);
    java.util.Optional<Cv> findByCandidateIdAndIsDefaultTrueAndStatus(Long candidateId, String status);
}