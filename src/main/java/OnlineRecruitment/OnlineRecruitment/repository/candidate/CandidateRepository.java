package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Lấy từ code của nhóm (HEAD)
    Optional<Candidate> findByUserId(Long userId);
    Optional<Candidate> findByUser(User user);

    // Thêm comment của bạn vào (không bắt buộc, nhưng giữ lại cho rõ ràng)
    // TODO: TV1 review — method bổ sung cho module CV (TV3), cần để map user -> candidate

}