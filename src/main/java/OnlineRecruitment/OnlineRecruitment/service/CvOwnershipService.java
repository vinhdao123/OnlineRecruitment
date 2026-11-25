package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra quyền sở hữu CV: dùng chung cho CvService và các service tiểu mục,
 * tránh phụ thuộc vòng giữa các service của module CV.
 */
@Service
@RequiredArgsConstructor
public class CvOwnershipService {

    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;

    @Transactional(readOnly = true)
    public Candidate getCandidate(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ ứng viên"));
    }

    /** Trả về CV nếu thuộc về ứng viên của userId, ngược lại ném IllegalArgumentException. */
    @Transactional(readOnly = true)
    public Cv getOwnedCv(Long cvId, Long userId) {
        Candidate candidate = getCandidate(userId);
        return cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV hoặc bạn không có quyền truy cập"));
    }
}
