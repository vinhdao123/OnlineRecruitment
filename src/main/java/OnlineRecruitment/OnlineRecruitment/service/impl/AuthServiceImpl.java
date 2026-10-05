package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.dto.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterRegisterDto;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.TokenRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import OnlineRecruitment.OnlineRecruitment.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CompanyRepository companyRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Trong AuthServiceImpl.java
    @Override
    @Transactional
    public void registerCandidate(CandidateRegisterDto dto) {
        // 1. Kiem tra email da ton tai chua
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống!");
        }

        // 2. Luu User / Candidate
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(false); // Chưa kích hoạt cho đến khi verify OTP
        userRepository.save(user);

        // 3. Sinh mã OTP 6 chữ số
        String otpCode = String.format("%06d", new java.util.Random().nextInt(900000) + 100000);

        // 4. Lưu VerificationToken
        VerificationToken token = new VerificationToken();
        token.setToken(otpCode);
        token.setUser(user);
        token.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        tokenRepository.save(token);

        // 5. Gửi Email OTP
        emailService.sendOtpEmail(user.getEmail(), otpCode);
    }

    @Override
    @Transactional
    public void registerRecruiter(RecruiterRegisterDto dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Tên người dùng đã tồn tại");
        }

        if (userRepository.existsByEmail(dto.getPersonalEmail())) {
            throw new IllegalArgumentException("Email cá nhân đã được sử dụng");
        }

        if (companyRepository.existsByTaxCode(dto.getTaxCode())) {
            throw new IllegalArgumentException("Mã số thuế đã tồn tại trên hệ thống");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getPersonalEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getUsername())
                .phone(dto.getCompanyPhone())
                .role(Role.RECRUITER)
                .status(UserStatus.INACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        Company company = Company.builder()
                .recruiter(savedUser)
                .companyName(dto.getCompanyName())
                .taxCode(dto.getTaxCode())
                .email(dto.getCompanyEmail())
                .phone(dto.getCompanyPhone())
                .companySize(dto.getCompanySize())
                .website(dto.getCompanyWebsite())
                .address(dto.getCompanyAddress())
                .description(dto.getCompanyDescription())
                .status(CompanyStatus.PENDING)
                .build();

        companyRepository.save(company);

        createAndSendVerificationToken(savedUser);
    }

    @Override
    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return false;
        }

        VerificationToken verificationToken = tokenRepository
                .findFirstByUserAndTokenTypeOrderByCreatedAtDesc(user, TokenType.EMAIL_VERIFICATION)
                .orElse(null);

        if (verificationToken == null || verificationToken.isUsed() || verificationToken.isExpired()) {
            return false;
        }

        // Token format: OTP_UUID
        String tokenStr = verificationToken.getToken();
        if (tokenStr.startsWith(otp + "_")) {
            verificationToken.setUsedAt(LocalDateTime.now());
            tokenRepository.save(verificationToken);

            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public boolean verifyByToken(String token) {
        VerificationToken verificationToken = tokenRepository
                .findByTokenAndTokenType(token, TokenType.EMAIL_VERIFICATION)
                .orElse(null);

        if (verificationToken == null || verificationToken.isUsed() || verificationToken.isExpired()) {
            return false;
        }

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + email));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Tài khoản này đã được xác thực trước đó.");
        }

        createAndSendVerificationToken(user);
    }

    private void createAndSendVerificationToken(User user) {
        String otpCode = String.format("%06d", RANDOM.nextInt(1_000_000));
        String fullToken = otpCode + "_" + UUID.randomUUID().toString();

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(fullToken)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(token);

        String verificationLink = baseUrl + "/auth/verify?token=" + fullToken;
        emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), otpCode, verificationLink);
    }
}
