package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.ApplicationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationAttemptRepository
        extends JpaRepository<ApplicationAttempt, Long> {

    Optional<ApplicationAttempt> findByCandidate_IdAndJob_Id(
            Long candidateId,
            Long jobId
    );
}
