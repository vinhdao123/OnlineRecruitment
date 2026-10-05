package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterRegisterDto;

public interface AuthService {
    void registerCandidate(CandidateRegisterDto dto);
    void registerRecruiter(RecruiterRegisterDto dto);
    boolean verifyEmailOtp(String email, String otp);
    boolean verifyByToken(String token);
    void resendVerificationOtp(String email);
}
