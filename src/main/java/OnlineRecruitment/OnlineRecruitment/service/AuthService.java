package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterRegisterDto;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CompanyRepository companyRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // 1. Đăng ký Candidate
    @Transactional
    public void registerCandidate(CandidateRegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setLocation(dto.getAddress());
        user.setRole(UserRole.CANDIDATE);
        user.setStatus(UserStatus.PENDING); // Đợi xác thực OTP
        userRepository.save(user);

        Candidate candidate = new Candidate();
        candidate.setUser(user);
        candidate.setDob(dto.getDob());
        candidate.setGender(dto.getGender());
        candidate.setAddress(dto.getAddress());
        candidateRepository.save(candidate);

        generateAndSendOtp(user.getEmail(), user);
    }

    // 2. Đăng ký Recruiter
    @Transactional
    public void registerRecruiter(RecruiterRegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email tài khoản đã tồn tại!");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username đã được sử dụng!");
        }
        if (companyRepository.existsByTaxCode(dto.getTaxCode())) {
            throw new IllegalArgumentException("Mã số thuế công ty đã được đăng ký!");
        }

        // Tạo tài khoản User Recruiter
        User recruiter = new User();
        recruiter.setUsername(dto.getUsername());
        recruiter.setEmail(dto.getEmail());
        recruiter.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        recruiter.setFullName(dto.getFullName());
        recruiter.setPhone(dto.getPhone());
        recruiter.setRole(UserRole.RECRUITER);
        recruiter.setStatus(UserStatus.PENDING); // Chờ xác thực OTP + Admin duyệt
        userRepository.save(recruiter);

        // Tạo thông tin Công ty
        Company company = new Company();
        company.setRecruiter(recruiter);
        company.setCompanyName(dto.getCompanyName());
        company.setTaxCode(dto.getTaxCode());
        company.setEmail(dto.getCompanyEmail());
        company.setPhone(dto.getCompanyPhone());
        company.setAddress(dto.getCompanyAddress());
        company.setWebsite(dto.getCompanyWebsite());
        company.setCompanySize(dto.getCompanySize());
        company.setDescription(dto.getCompanyDescription());
        company.setStatus(CompanyStatus.PENDING); // Chờ Admin duyệt
        companyRepository.save(company);

        generateAndSendOtp(recruiter.getEmail(), recruiter);
    }

    // 3. Tạo & Gửi OTP (dùng SecureRandom, SHA-256 byte[])
    public void generateAndSendOtp(String email, User user) {
        // Cooldown check
        otpTokenRepository.findTopByEmailOrderByCreatedAtDesc(email).ifPresent(lastToken -> {
            if (lastToken.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(RESEND_COOLDOWN_SECONDS))) {
                throw new IllegalArgumentException("Vui lòng đợi " + RESEND_COOLDOWN_SECONDS + " giây trước khi yêu cầu mã mới!");
            }
        });

        int code = secureRandom.nextInt(900000) + 100000; // Đảm bảo 6 chữ số
        String otp = String.valueOf(code);

        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setUser(user);
        otpToken.setOtpHash(hashOtp(otp));
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otpToken.setAttemptCount(0);
        otpTokenRepository.save(otpToken);

        emailService.sendOtpEmail(email, "Mã xác thực OTP đăng ký tài khoản",
                "Mã OTP của bạn là: " + otp + " (Có hiệu lực trong 10 phút). Không chia sẻ mã này cho bất kỳ ai.");
    }

    // Gửi lại OTP
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + email));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản này đã được kích hoạt.");
        }

        generateAndSendOtp(email, user);
    }

    // 4. Verify OTP
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        OtpToken token = otpTokenRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã OTP cho email này."));

        if (token.getUsedAt() != null) {
            throw new IllegalArgumentException("Mã OTP này đã được sử dụng. Vui lòng gửi lại mã mới.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }

        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Bạn đã nhập sai quá " + MAX_ATTEMPTS + " lần. Vui lòng yêu cầu mã OTP mới.");
        }

        byte[] inputHash = hashOtp(otp);
        if (!MessageDigest.isEqual(token.getOtpHash(), inputHash)) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            otpTokenRepository.save(token);
            throw new IllegalArgumentException("Mã OTP không chính xác. Số lần thử còn lại: " + (MAX_ATTEMPTS - token.getAttemptCount()));
        }

        // OTP hợp lệ
        token.setUsedAt(LocalDateTime.now());
        otpTokenRepository.save(token);

        User user = token.getUser();
        if (user != null) {
            if (user.getRole() == UserRole.CANDIDATE) {
                user.setStatus(UserStatus.ACTIVE); // Candidate được kích hoạt ngay
                userRepository.save(user);
            } else if (user.getRole() == UserRole.RECRUITER) {
                // Recruiter vẫn PENDING chờ Admin duyệt, thông báo cho tất cả Admin đang active
                List<User> activeAdmins = userRepository.findByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
                String companyName = user.getCompany() != null ? user.getCompany().getCompanyName() : "Chưa rõ";
                for (User admin : activeAdmins) {
                    emailService.sendEmail(admin.getEmail(),
                            "Yêu cầu duyệt công ty mới: " + companyName,
                            "Recruiter: " + user.getFullName() + " (" + user.getEmail() + ") vừa hoàn tất xác minh OTP. Vui lòng vào trang Admin để duyệt.");
                }
            }
        }
        return true;
    }

    private byte[] hashOtp(String otp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(otp.trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}