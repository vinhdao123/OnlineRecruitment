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
}