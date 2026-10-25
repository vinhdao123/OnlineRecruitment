package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.OtpTokenRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterProfileDto;
import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterResubmitDto;
import OnlineRecruitment.OnlineRecruitment.entity.OtpToken;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final ActivityLogService activityLogService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<Company> search(String keyword, CompanyStatus status, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return companyRepository.search(
                kw, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    @Transactional(readOnly = true)
    public Company getById(Long id) {
        return companyRepository.findByIdWithRecruiter(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Company> findByRecruiterId(Long recruiterId) {
        return companyRepository.findByRecruiterId(recruiterId);
    }

    @Transactional
    public void approve(Long id, User actor) {
        Company c = getById(id);
        User recruiter = c.getRecruiter();

        if (recruiter != null) {
            boolean hasVerifiedOtp = otpTokenRepository.existsByEmailAndUsedAtIsNotNull(recruiter.getEmail());
            if (!hasVerifiedOtp) {
                throw new IllegalStateException("Không thể duyệt công ty: Người đại diện (Recruiter) chưa xác thực mã OTP!");
            }
            recruiter.setStatus(UserStatus.ACTIVE);
            userRepository.save(recruiter);
        }

        c.setStatus(CompanyStatus.ACTIVE);
        c.setRejectionReason(null);
        companyRepository.save(c);

        if (recruiter != null) {
            emailService.sendEmail(recruiter.getEmail(),
                    "Thông báo: Hồ sơ doanh nghiệp đã được phê duyệt!",
                    "Xin chào " + recruiter.getFullName() + ",\n\nCông ty " + c.getCompanyName() +
                    " của bạn đã được Admin phê duyệt thành công. Bạn hiện có thể đăng nhập và đăng tin tuyển dụng.");
        }

        activityLogService.log(actor, "APPROVE_COMPANY",
                "Đã duyệt công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "System Update");
    }

    private byte[] hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(token.trim().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }

    @Transactional
    public void reject(Long id, String reason, User actor) {
        Company c = getById(id);
        User recruiter = c.getRecruiter();

        String finalReason = (reason != null && !reason.trim().isBlank())
                ? reason.trim()
                : "Hồ sơ chưa đạt yêu cầu xét duyệt của hệ thống. Vui lòng kiểm tra và cập nhật lại thông tin đăng ký doanh nghiệp.";

        c.setStatus(CompanyStatus.REJECTED);
        c.setRejectionReason(finalReason);
        companyRepository.save(c);

        if (recruiter != null) {
            recruiter.setStatus(UserStatus.REJECTED);
            recruiter.setRejectionReason(finalReason);
            userRepository.save(recruiter);

            // Tạo mã token để cho phép Recruiter chỉnh sửa lại form mà không cần đăng ký lại từ đầu
            String resubmitToken = UUID.randomUUID().toString();
            OtpToken otpToken = new OtpToken();
            otpToken.setEmail(recruiter.getEmail());
            otpToken.setUser(recruiter);
            otpToken.setOtpHash(hashToken(resubmitToken));
            otpToken.setExpiresAt(LocalDateTime.now().plusDays(7));
            otpToken.setAttemptCount(0);
            otpTokenRepository.save(otpToken);

            String encodedEmail = URLEncoder.encode(recruiter.getEmail(), StandardCharsets.UTF_8);
            String cleanBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "http://localhost:8080";
            String resubmitUrl = cleanBaseUrl + "/register/recruiter/resubmit?email=" + encodedEmail + "&token=" + resubmitToken;

            String emailContent = "Xin chào " + recruiter.getFullName() + ",\n\n"
                    + "Rất tiếc, hồ sơ đăng ký doanh nghiệp của công ty \"" + c.getCompanyName() + "\" chưa được phê duyệt.\n\n"
                    + "LÝ DO TỪ CHỐI:\n"
                    + "--------------------------------------------------\n"
                    + finalReason + "\n"
                    + "--------------------------------------------------\n\n"
                    + "Bạn KHÔNG CẦN phải đăng ký lại một tài khoản mới từ đầu.\n"
                    + "Chúng tôi đã lưu lại toàn bộ thông tin đăng ký trước đó của bạn. Vui lòng nhấn vào liên kết bên dưới để chỉnh sửa lại những thông tin chưa chính xác và gửi lại yêu cầu xét duyệt:\n\n"
                    + "👉 Đường dẫn chỉnh sửa hồ sơ: " + resubmitUrl + "\n\n"
                    + "(Liên kết này có hiệu lực trong vòng 7 ngày).\n\n"
                    + "Trân trọng,\n"
                    + "Ban quản trị Online Recruitment";

            emailService.sendEmail(recruiter.getEmail(),
                    "Thông báo từ chối hồ sơ doanh nghiệp & Hướng dẫn chỉnh sửa - " + c.getCompanyName(),
                    emailContent);
        }

        activityLogService.log(actor, "REJECT_COMPANY",
                "Đã từ chối công ty: " + c.getCompanyName() + ". Lý do: " + finalReason,
                "COMPANY", c.getId(), "System Update");
    }

    @Transactional(readOnly = true)
    public Company getCompanyForResubmit(String email, String token) {
        if (email == null || email.isBlank() || token == null || token.isBlank()) {
            throw new IllegalArgumentException("Thông tin liên kết không hợp lệ!");
        }

        User recruiter = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người dùng với email: " + email));

        Company company = companyRepository.findByRecruiterId(recruiter.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin công ty tương ứng."));

        OtpToken otpToken = otpTokenRepository.findTopByEmailOrderByCreatedAtDesc(recruiter.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã xác thực chỉnh sửa hoặc liên kết đã hết hạn."));

        if (otpToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Liên kết chỉnh sửa này đã được sử dụng. Nếu bạn đã gửi lại hồ sơ, vui lòng chờ quản trị viên xét duyệt.");
        }

        if (otpToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Liên kết chỉnh sửa này đã hết hạn (quá 7 ngày). Vui lòng liên hệ quản trị viên.");
        }

        byte[] inputHash = hashToken(token);
        if (!MessageDigest.isEqual(otpToken.getOtpHash(), inputHash)) {
            throw new IllegalArgumentException("Mã xác thực liên kết không chính xác hoặc không hợp lệ!");
        }

        return company;
    }

    @Transactional
    public void resubmitCompany(String email, String token, RecruiterResubmitDto dto) {
        Company company = getCompanyForResubmit(email, token);
        User recruiter = company.getRecruiter();

        // Kiểm tra mã số thuế mới nếu có thay đổi
        if (!company.getTaxCode().equalsIgnoreCase(dto.getTaxCode().trim())) {
            if (companyRepository.existsByTaxCode(dto.getTaxCode().trim())) {
                throw new IllegalArgumentException("Mã số thuế này đã được đăng ký bởi doanh nghiệp khác!");
            }
        }

        // Cập nhật thông tin Recruiter
        recruiter.setFullName(dto.getFullName().trim());
        recruiter.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 6) {
                throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 6 ký tự!");
            }
            if (!dto.getPassword().equals(dto.getConfirmPassword())) {
                throw new IllegalArgumentException("Mật khẩu xác nhận không khớp!");
            }
            recruiter.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        recruiter.setStatus(UserStatus.PENDING);
        recruiter.setRejectionReason(null);
        recruiter.setUpdatedAt(LocalDateTime.now());
        userRepository.save(recruiter);

        // Cập nhật thông tin Công ty
        company.setCompanyName(dto.getCompanyName().trim());
        company.setTaxCode(dto.getTaxCode().trim());
        company.setEmail(dto.getCompanyEmail() != null ? dto.getCompanyEmail().trim() : null);
        company.setPhone(dto.getCompanyPhone() != null ? dto.getCompanyPhone().trim() : null);
        company.setAddress(dto.getCompanyAddress() != null ? dto.getCompanyAddress().trim() : null);
        company.setWebsite(dto.getCompanyWebsite() != null ? dto.getCompanyWebsite().trim() : null);
        company.setCompanySize(dto.getCompanySize());
        company.setDescription(dto.getCompanyDescription());
        company.setStatus(CompanyStatus.PENDING);
        company.setRejectionReason(null);
        company.setUpdatedAt(LocalDateTime.now());
        companyRepository.save(company);

        // Đánh dấu token đã dùng
        OtpToken otpToken = otpTokenRepository.findTopByEmailOrderByCreatedAtDesc(recruiter.getEmail()).orElse(null);
        if (otpToken != null) {
            otpToken.setUsedAt(LocalDateTime.now());
            otpTokenRepository.save(otpToken);
        }

        // Ghi log
        activityLogService.log(recruiter, "RESUBMIT_COMPANY",
                "Đã chỉnh sửa và gửi lại hồ sơ công ty: " + company.getCompanyName(),
                "COMPANY", company.getId(), "Recruiter Resubmission");

        // Gửi email cho các Admin
        List<User> activeAdmins = userRepository.findByRoleAndStatus(UserRole.ADMIN, UserStatus.ACTIVE);
        for (User admin : activeAdmins) {
            emailService.sendEmail(admin.getEmail(),
                    "Yêu cầu xét duyệt lại: Công ty " + company.getCompanyName(),
                    "Nhà tuyển dụng " + recruiter.getFullName() + " (" + recruiter.getEmail() + ") vừa chỉnh sửa và gửi lại hồ sơ công ty \""
                            + company.getCompanyName() + "\".\nVui lòng vào trang quản trị để xem xét duyệt.");
        }
    }

    @Transactional
    public void lock(Long id, User actor) {
        Company c = getById(id);
        c.setStatus(CompanyStatus.LOCKED);
        companyRepository.save(c);

        User recruiter = c.getRecruiter();
        if (recruiter != null) {
            recruiter.setStatus(UserStatus.LOCKED);
            userRepository.save(recruiter);
        }

        activityLogService.log(actor, "LOCK_COMPANY",
                "Đã khóa công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "Suspicious Activity");
    }

    @Transactional
    public void unlock(Long id, User actor) {
        Company c = getById(id);
        c.setStatus(CompanyStatus.ACTIVE);
        companyRepository.save(c);

        User recruiter = c.getRecruiter();
        if (recruiter != null) {
            recruiter.setStatus(UserStatus.ACTIVE);
            userRepository.save(recruiter);
        }

        activityLogService.log(actor, "UNLOCK_COMPANY",
                "Đã mở khóa công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "System Update");
    }

    @Transactional
    public void updateRecruiterProfile(User recruiter, RecruiterProfileDto dto) throws IOException {
        recruiter.setFullName(dto.getFullName().trim());
        recruiter.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        recruiter.setLocation(dto.getLocation() != null ? dto.getLocation().trim() : null);

        if (dto.getAvatarFile() != null && !dto.getAvatarFile().isEmpty()) {
            String avatarUrl = fileStorageService.saveAvatar(dto.getAvatarFile());
            recruiter.setAvatarUrl(avatarUrl);
        }
        userRepository.save(recruiter);

        Company company = companyRepository.findByRecruiterId(recruiter.getId())
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setRecruiter(recruiter);
                    c.setStatus(CompanyStatus.ACTIVE);
                    return c;
                });

        company.setCompanyName(dto.getCompanyName().trim());
        if (dto.getTaxCode() != null && !dto.getTaxCode().isBlank()) {
            company.setTaxCode(dto.getTaxCode().trim());
        }
        company.setEmail(dto.getCompanyEmail() != null ? dto.getCompanyEmail().trim() : null);
        company.setPhone(dto.getCompanyPhone() != null ? dto.getCompanyPhone().trim() : null);
        company.setAddress(dto.getCompanyAddress() != null ? dto.getCompanyAddress().trim() : null);
        company.setWebsite(dto.getCompanyWebsite() != null ? dto.getCompanyWebsite().trim() : null);
        company.setCompanySize(dto.getCompanySize());
        company.setDescription(dto.getCompanyDescription());

        if (dto.getLogoFile() != null && !dto.getLogoFile().isEmpty()) {
            String logoUrl = fileStorageService.saveCompanyLogo(dto.getLogoFile());
            company.setLogoUrl(logoUrl);
        }

        companyRepository.save(company);
    }

    public long countAll()                     { return companyRepository.count(); }
    public long countByStatus(CompanyStatus s) { return companyRepository.countByStatus(s); }
}
