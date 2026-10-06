package OnlineRecruitment.OnlineRecruitment.service.impl;

import OnlineRecruitment.OnlineRecruitment.dto.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterRegisterDto;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.Role;
import OnlineRecruitment.OnlineRecruitment.entity.TokenType;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.entity.VerificationToken;
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

import java.time.LocalDateTime;
import java.security.SecureRandom;
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
    // 1. REGISTER CANDIDATE
    // =========================================================

    @Override
    @Transactional
    public void registerCandidate(CandidateRegisterDto dto) {

        // Kiểm tra mật khẩu xác nhận
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException(
                    "Email đã tồn tại trong hệ thống!"
            );
        }

        String username = dto.getEmail();

        // Kiểm tra username
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Tên người dùng đã tồn tại trong hệ thống!"
            );
        }

        // -----------------------------------------------------
        // Tạo User
        // -----------------------------------------------------

        User user = User.builder()
                .username(username)
                .email(dto.getEmail())
                .passwordHash(
                        passwordEncoder.encode(dto.getPassword())
                )
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .role(Role.CANDIDATE)
                .status(UserStatus.INACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // -----------------------------------------------------
        // Tạo Candidate
        // -----------------------------------------------------

        Candidate candidate = Candidate.builder()
                .user(savedUser)
                .dob(dto.getDob())
                .gender(dto.getGender())
                .address(dto.getAddress())
                .build();

        candidateRepository.save(candidate);

        // -----------------------------------------------------
        // Tạo OTP + Verification Token
        // -----------------------------------------------------

        createAndSendVerificationToken(savedUser);

        log.info(
                "Candidate registration successful: {}",
                savedUser.getEmail()
        );
    }

    // =========================================================
    // 2. REGISTER RECRUITER
    // =========================================================

    @Override
    @Transactional
    public void registerRecruiter(RecruiterRegisterDto dto) {

        // Kiểm tra confirm password
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException(
                    "Tên người dùng đã tồn tại"
            );
        }

        if (userRepository.existsByEmail(dto.getPersonalEmail())) {
            throw new IllegalArgumentException(
                    "Email cá nhân đã được sử dụng"
            );
        }

        if (companyRepository.existsByTaxCode(dto.getTaxCode())) {
            throw new IllegalArgumentException(
                    "Mã số thuế đã tồn tại trên hệ thống"
            );
        }

        // -----------------------------------------------------
        // Tạo User Recruiter
        // -----------------------------------------------------

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getPersonalEmail())
                .passwordHash(
                        passwordEncoder.encode(dto.getPassword())
                )
                .fullName(dto.getUsername())
                .phone(dto.getCompanyPhone())
                .role(Role.RECRUITER)
                .status(UserStatus.INACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // -----------------------------------------------------
        // Tạo Company
        // -----------------------------------------------------

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

        // -----------------------------------------------------
        // Gửi OTP
        // -----------------------------------------------------

        createAndSendVerificationToken(savedUser);

        log.info(
                "Recruiter registration successful: {}",
                savedUser.getEmail()
        );
    }

    // =========================================================
    // 3. VERIFY OTP
    // =========================================================

    @Override
    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            log.warn(
                    "OTP verification failed - user not found: {}",
                    email
            );
            return false;
        }

        VerificationToken verificationToken =
                tokenRepository
                        .findFirstByUserAndTokenTypeOrderByCreatedAtDesc(
                                user,
                                TokenType.EMAIL_VERIFICATION
                        )
                        .orElse(null);

        if (verificationToken == null) {
            log.warn(
                    "OTP verification failed - token not found: {}",
                    email
            );
            return false;
        }

        if (verificationToken.isUsed()) {
            log.warn(
                    "OTP verification failed - token already used: {}",
                    email
            );
            return false;
        }

        if (verificationToken.isExpired()) {
            log.warn(
                    "OTP verification failed - token expired: {}",
                    email
            );
            return false;
        }

        String token = verificationToken.getToken();

        if (token == null || !token.startsWith(otp + "_")) {
            log.warn(
                    "OTP verification failed - wrong OTP: {}",
                    email
            );
            return false;
        }

        // -----------------------------------------------------
        // OTP đúng
        // -----------------------------------------------------

        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info(
                "Email verified successfully: {}",
                email
        );

        return true;
    }

    // =========================================================
    // 4. VERIFY BY EMAIL LINK
    // =========================================================

    @Override
    @Transactional
    public boolean verifyByToken(String token) {

        VerificationToken verificationToken =
                tokenRepository
                        .findByTokenAndTokenType(
                                token,
                                TokenType.EMAIL_VERIFICATION
                        )
                        .orElse(null);

        if (verificationToken == null) {
            log.warn(
                    "Verification failed - token not found"
            );
            return false;
        }

        if (verificationToken.isUsed()) {
            log.warn(
                    "Verification failed - token already used"
            );
            return false;
        }

        if (verificationToken.isExpired()) {
            log.warn(
                    "Verification failed - token expired"
            );
            return false;
        }

        // Đánh dấu token đã sử dụng
        verificationToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(verificationToken);

        // Kích hoạt user
        User user = verificationToken.getUser();

        if (user == null) {
            log.error(
                    "Verification token has no associated user"
            );
            return false;
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info(
                "Email verified successfully by link: {}",
                user.getEmail()
        );

        return true;
    }

    // =========================================================
    // 5. RESEND OTP
    // =========================================================

    @Override
    @Transactional
    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy người dùng với email: "
                                        + email
                        )
                );

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Tài khoản này đã được xác thực trước đó."
            );
        }

        createAndSendVerificationToken(user);

        log.info(
                "OTP resent successfully: {}",
                email
        );
    }

    // =========================================================
    // 6. CREATE + SEND VERIFICATION TOKEN
    // =========================================================

    private void createAndSendVerificationToken(User user) {

        String otpCode = String.format(
                "%06d",
                RANDOM.nextInt(1_000_000)
        );

        String fullToken =
                otpCode + "_" + UUID.randomUUID();

        VerificationToken token =
                VerificationToken.builder()
                        .user(user)
                        .token(fullToken)
                        .tokenType(
                                TokenType.EMAIL_VERIFICATION
                        )
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(15)
                        )
                        .build();

        tokenRepository.save(token);

        String verificationLink =
                baseUrl
                        + "/auth/verify?token="
                        + fullToken;

        // Gửi email
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                otpCode,
                verificationLink
        );

        log.info(
                "Verification token created for: {}",
                user.getEmail()
        );
    }
}

