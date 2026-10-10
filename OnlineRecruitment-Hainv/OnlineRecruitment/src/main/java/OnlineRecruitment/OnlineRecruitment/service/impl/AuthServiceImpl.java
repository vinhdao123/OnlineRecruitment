package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.dto.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterRegisterDto;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.TokenType;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.entity.VerificationToken;

import OnlineRecruitment.OnlineRecruitment.repository.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.TokenRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;

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
import java.util.Locale;
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

    // =========================================================
    // 1. ĐĂNG KÝ CANDIDATE
    // =========================================================

    @Override
    @Transactional
    public void registerCandidate(CandidateRegisterDto dto) {

        String email = dto.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp."
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email đã tồn tại trong hệ thống."
            );
        }

        String username = generateUniqueUsername(email);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(dto.getPassword())
        );
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setLocation(dto.getAddress().trim());
        user.setRole(UserRole.CANDIDATE);
        user.setStatus(UserStatus.INACTIVE);

        User savedUser = userRepository.save(user);

        Candidate candidate = Candidate.builder()
                .user(savedUser)
                .dob(dto.getDob())
                .gender(dto.getGender())
                .address(dto.getAddress().trim())
                .build();

        candidateRepository.save(candidate);

        createAndSendVerificationToken(savedUser);

        log.info(
                "Đã tạo tài khoản Candidate cho email {}",
                email
        );
    }

    // =========================================================
    // 2. ĐĂNG KÝ RECRUITER
    // =========================================================

    @Override
    @Transactional
    public void registerRecruiter(RecruiterRegisterDto dto) {

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp."
            );
        }

        String username = dto.getUsername().trim();

        // RecruiterRegisterDto dùng getEmail()
        String personalEmail = dto.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Tên người dùng đã tồn tại."
            );
        }

        if (userRepository.existsByEmail(personalEmail)) {
            throw new IllegalArgumentException(
                    "Email cá nhân đã được sử dụng."
            );
        }

        String taxCode = dto.getTaxCode().trim();

        if (companyRepository.existsByTaxCode(taxCode)) {
            throw new IllegalArgumentException(
                    "Mã số thuế đã tồn tại trên hệ thống."
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(personalEmail);
        user.setPasswordHash(
                passwordEncoder.encode(dto.getPassword())
        );
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone().trim());
        user.setRole(UserRole.RECRUITER);
        user.setStatus(UserStatus.INACTIVE);

        User savedUser = userRepository.save(user);

        // Các tên getter khớp với RecruiterRegisterDto hiện tại.
        Company company = Company.builder()
                .recruiter(savedUser)
                .companyName(dto.getCompanyName().trim())
                .taxCode(taxCode)
                .email(dto.getCompanyEmail().trim())
                .phone(dto.getCompanyPhone().trim())
                .companySize(dto.getCompanySize())
                .website(dto.getWebsite())
                .address(dto.getAddress().trim())
                .description(dto.getCompanyDescription())
                .status(CompanyStatus.PENDING)
                .build();

        companyRepository.save(company);

        createAndSendVerificationToken(savedUser);

        log.info(
                "Đã tạo tài khoản Recruiter cho email {}",
                personalEmail
        );
    }

    // =========================================================
    // 3. XÁC MINH EMAIL BẰNG OTP
    // =========================================================

    @Override
    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {

        if (email == null || email.isBlank()
                || otp == null
                || !otp.matches("\\d{6}")) {
            return false;
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getStatus() == UserStatus.ACTIVE) {
            return true;
        }

        VerificationToken verificationToken = tokenRepository
                .findFirstByUserAndTokenTypeOrderByCreatedAtDesc(
                        user,
                        TokenType.EMAIL_VERIFICATION
                )
                .orElse(null);

        if (verificationToken == null
                || verificationToken.isUsed()
                || verificationToken.isExpired()) {
            return false;
        }

        String tokenValue = verificationToken.getToken();

        if (tokenValue == null
                || !tokenValue.startsWith(otp + "_")) {
            return false;
        }

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info(
                "Xác minh OTP thành công cho email {}",
                normalizedEmail
        );

        return true;
    }

    // =========================================================
    // 4. XÁC MINH EMAIL BẰNG LINK
    // =========================================================

    @Override
    @Transactional
    public boolean verifyByToken(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        VerificationToken verificationToken = tokenRepository
                .findByTokenAndTokenType(
                        token,
                        TokenType.EMAIL_VERIFICATION
                )
                .orElse(null);

        if (verificationToken == null
                || verificationToken.isUsed()
                || verificationToken.isExpired()) {
            return false;
        }

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info(
                "Xác minh email bằng link thành công cho {}",
                user.getEmail()
        );

        return true;
    }

    // =========================================================
    // 5. GỬI LẠI OTP
    // =========================================================

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Vui lòng nhập email."
            );
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tài khoản với email này."
                        )
                );

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Tài khoản này đã được xác thực."
            );
        }

        createAndSendVerificationToken(user);

        log.info(
                "Đã gửi lại OTP cho {}",
                normalizedEmail
        );
    }

    // =========================================================
    // 6. TẠO USERNAME TỰ ĐỘNG CHO CANDIDATE
    // =========================================================

    private String generateUniqueUsername(String email) {

        String localPart = email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : email;

        String base = localPart.replaceAll(
                "[^a-zA-Z0-9._-]",
                ""
        );

        if (base.isBlank()) {
            base = "candidate";
        }

        if (base.length() > 85) {
            base = base.substring(0, 85);
        }

        String username = base;
        int suffix = 1;

        while (userRepository.existsByUsername(username)) {
            username = base + suffix;
            suffix++;
        }

        return username;
    }

    // =========================================================
    // 7. TẠO TOKEN VÀ GỬI EMAIL
    // =========================================================

    private void createAndSendVerificationToken(User user) {

        String otpCode = String.format(
                "%06d",
                RANDOM.nextInt(1_000_000)
        );

        String fullToken = otpCode + "_" + UUID.randomUUID();

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .token(fullToken)
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        tokenRepository.save(token);

        String verificationLink = baseUrl
                + "/auth/verify?token="
                + fullToken;

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                otpCode,
                verificationLink
        );
    }
}