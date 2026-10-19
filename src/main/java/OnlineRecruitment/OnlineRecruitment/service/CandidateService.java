package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CandidateProfileDto;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public Candidate getOrCreateCandidate(User user) {
        return candidateRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Candidate newCandidate = new Candidate();
                    newCandidate.setUser(user);
                    return newCandidate;
                });
    }

    @Transactional
    public void updateProfile(User user, CandidateProfileDto dto) throws IOException {
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setLocation(dto.getAddress() != null ? dto.getAddress().trim() : null);

        // Upload avatar nếu có
        if (dto.getAvatarFile() != null && !dto.getAvatarFile().isEmpty()) {
            String avatarUrl = fileStorageService.saveAvatar(dto.getAvatarFile());
            user.setAvatarUrl(avatarUrl);
        }
        userRepository.save(user);

        Candidate candidate = candidateRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Candidate c = new Candidate();
                    c.setUser(user);
                    return c;
                });

        candidate.setDesiredPosition(dto.getDesiredPosition() != null ? dto.getDesiredPosition().trim() : null);
        candidate.setAboutMe(dto.getAboutMe() != null ? dto.getAboutMe().trim() : null);
        candidate.setLinkedinUrl(dto.getLinkedinUrl() != null ? dto.getLinkedinUrl().trim() : null);
        candidate.setGithubUrl(dto.getGithubUrl() != null ? dto.getGithubUrl().trim() : null);
        candidate.setPortfolioUrl(dto.getPortfolioUrl() != null ? dto.getPortfolioUrl().trim() : null);
        candidate.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        candidate.setDob(dto.getDob());
        candidate.setGender(dto.getGender());

        candidateRepository.save(candidate);
    }
}
