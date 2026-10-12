package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import OnlineRecruitment.OnlineRecruitment.dto.CompanyProfileUpdateDto;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ActivityLogService activityLogService;

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
        c.setStatus(CompanyStatus.APPROVED);
        c.setRejectionReason(null);
        companyRepository.save(c);

        activityLogService.log(actor, "APPROVE_COMPANY",
                "Đã duyệt công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "System Update");
    }

    @Transactional
    public void reject(Long id, String reason, User actor) {
        Company c = getById(id);
        c.setStatus(CompanyStatus.REJECTED);
        c.setRejectionReason(reason);
        companyRepository.save(c);

        activityLogService.log(actor, "REJECT_COMPANY",
                "Đã từ chối công ty: " + c.getCompanyName() + ". Lý do: " + reason,
                "COMPANY", c.getId(), "System Update");
    }

    @Transactional
    public void lock(Long id, User actor) {
        Company c = getById(id);
        c.setStatus(CompanyStatus.INACTIVE);
        companyRepository.save(c);

        activityLogService.log(actor, "LOCK_COMPANY",
                "Đã khóa công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "Suspicious Activity");
    }

    @Transactional
    public void unlock(Long id, User actor) {
        Company c = getById(id);
        c.setStatus(CompanyStatus.APPROVED);
        companyRepository.save(c);

        activityLogService.log(actor, "UNLOCK_COMPANY",
                "Đã mở khóa công ty: " + c.getCompanyName(),
                "COMPANY", c.getId(), "System Update");
    }

    public long countAll()                     { return companyRepository.count(); }
    public long countByStatus(CompanyStatus s) { return companyRepository.countByStatus(s); }
    @Transactional
    public Company updateRecruiterCompany(
            Long recruiterId,
            CompanyProfileUpdateDto dto
    ) {
        User recruiter = companyRepository
                .findByRecruiterId(recruiterId)
                .map(Company::getRecruiter)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có hồ sơ công ty"
                ));

        if (recruiter.getRole() != UserRole.RECRUITER) {
            throw new IllegalArgumentException(
                    "Tài khoản không có quyền recruiter"
            );
        }

        Company company = companyRepository
                .findByRecruiterId(recruiterId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy công ty của bạn"
                ));

        String taxCode = dto.taxCode().trim();

        if (!taxCode.equals(company.getTaxCode())
                && companyRepository.existsByTaxCode(taxCode)) {
            throw new IllegalArgumentException(
                    "Mã số thuế đã được sử dụng"
            );
        }

        company.setCompanyName(dto.companyName().trim());
        company.setTaxCode(taxCode);
        company.setEmail(dto.email().trim().toLowerCase(
                java.util.Locale.ROOT
        ));
        company.setPhone(normalizeCompanyField(dto.phone()));
        company.setAddress(dto.address().trim());
        company.setCompanySize(normalizeCompanyField(dto.companySize()));
        company.setWebsite(normalizeCompanyField(dto.website()));
        company.setDescription(normalizeCompanyField(dto.description()));
        company.setLogoUrl(normalizeCompanyField(dto.logoUrl()));

        // Không thay đổi recruiter, status hoặc rejectionReason.
        return companyRepository.save(company);
    }

    private String normalizeCompanyField(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}