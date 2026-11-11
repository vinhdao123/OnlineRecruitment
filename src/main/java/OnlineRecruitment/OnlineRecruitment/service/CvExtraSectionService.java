package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Các tiểu mục bổ sung của CV: Chứng chỉ, Giải thưởng, Hoạt động ngoại khóa, Người tham chiếu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvExtraSectionService {

    private final CvOwnershipService ownershipService;
    private final CvCertificateRepository certRepo;
    private final CvAwardRepository awardRepo;
    private final CvActivityRepository activityRepo;
    private final CvReferenceRepository referenceRepo;

    private void validateDateRange(LocalDate start, LocalDate end, boolean startNotFuture) {
        if (startNotFuture && start != null && start.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
    }

    // ---------- CHỨNG CHỈ ----------

    @Transactional(readOnly = true)
    public List<CvCertificate> getCertificates(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        try {
            return certRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch certificates for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addCertificate(Long cvId, CertificateRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        if (req.getIssueDate() != null && req.getExpiryDate() != null && req.getIssueDate().isAfter(req.getExpiryDate())) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau hoặc bằng ngày cấp.");
        }
        CvCertificate c = new CvCertificate();
        c.setCvId(cvId);
        c.setName(req.getName());
        c.setIssuer(req.getIssuer());
        c.setIssueDate(req.getIssueDate());
        c.setExpiryDate(req.getExpiryDate());
        c.setCredentialId(req.getCredentialId());
        c.setCredentialUrl(req.getCredentialUrl());
        c.setDescription(req.getDescription());
        c.setDisplayOrder(certRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        certRepo.save(c);
    }

    @Transactional
    public void deleteCertificate(Long cvId, Long certId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvCertificate c = certRepo.findById(certId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chứng chỉ"));
        if (!c.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        certRepo.delete(c);
    }

    // ---------- GIẢI THƯỞNG ----------

    @Transactional(readOnly = true)
    public List<CvAward> getAwards(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        try {
            return awardRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch awards for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addAward(Long cvId, AwardRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvAward a = new CvAward();
        a.setCvId(cvId);
        a.setTitle(req.getTitle());
        a.setIssuer(req.getIssuer());
        a.setAwardDate(req.getAwardDate());
        a.setDescription(req.getDescription());
        a.setDisplayOrder(awardRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        awardRepo.save(a);
    }

    @Transactional
    public void deleteAward(Long cvId, Long awardId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvAward a = awardRepo.findById(awardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giải thưởng"));
        if (!a.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        awardRepo.delete(a);
    }

    // ---------- HOẠT ĐỘNG NGOẠI KHÓA ----------

    @Transactional(readOnly = true)
    public List<CvActivity> getActivities(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        try {
            return activityRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch activities for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addActivity(Long cvId, ActivityRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        validateDateRange(req.getStartDate(), req.getEndDate(), true);
        boolean current = Boolean.TRUE.equals(req.getIsCurrent());
        CvActivity a = new CvActivity();
        a.setCvId(cvId);
        a.setOrganization(req.getOrganization());
        a.setRole(req.getRole());
        a.setStartDate(req.getStartDate());
        a.setEndDate(current ? null : req.getEndDate());
        a.setIsCurrent(current);
        a.setDescription(req.getDescription());
        a.setDisplayOrder(activityRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        activityRepo.save(a);
    }

    @Transactional
    public void deleteActivity(Long cvId, Long activityId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvActivity a = activityRepo.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hoạt động"));
        if (!a.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        activityRepo.delete(a);
    }

    // ---------- NGƯỜI THAM CHIẾU ----------

    @Transactional(readOnly = true)
    public List<CvReference> getReferences(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        try {
            return referenceRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch references for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addReference(Long cvId, ReferenceRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvReference r = new CvReference();
        r.setCvId(cvId);
        r.setFullName(req.getFullName());
        r.setPosition(req.getPosition());
        r.setCompany(req.getCompany());
        r.setEmail(req.getEmail());
        r.setPhone(req.getPhone());
        r.setRelationship(req.getRelationship());
        r.setDisplayOrder(referenceRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        referenceRepo.save(r);
    }

    @Transactional
    public void deleteReference(Long cvId, Long referenceId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        CvReference r = referenceRepo.findById(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người tham chiếu"));
        if (!r.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        referenceRepo.delete(r);
    }

    // ---------- SAO CHÉP SANG CV KHÁC ----------

    /** Sao chép chứng chỉ, giải thưởng, hoạt động, người tham chiếu từ CV nguồn sang CV đích. */
    @Transactional
    public void copySections(Long sourceCvId, Long targetCvId) {
        certRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(c -> {
            CvCertificate n = new CvCertificate();
            n.setCvId(targetCvId);
            n.setName(c.getName());
            n.setIssuer(c.getIssuer());
            n.setIssueDate(c.getIssueDate());
            n.setExpiryDate(c.getExpiryDate());
            n.setCredentialId(c.getCredentialId());
            n.setCredentialUrl(c.getCredentialUrl());
            n.setDescription(c.getDescription());
            n.setDisplayOrder(c.getDisplayOrder());
            certRepo.save(n);
        });

        awardRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(a -> {
            CvAward n = new CvAward();
            n.setCvId(targetCvId);
            n.setTitle(a.getTitle());
            n.setIssuer(a.getIssuer());
            n.setAwardDate(a.getAwardDate());
            n.setDescription(a.getDescription());
            n.setDisplayOrder(a.getDisplayOrder());
            awardRepo.save(n);
        });

        activityRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(a -> {
            CvActivity n = new CvActivity();
            n.setCvId(targetCvId);
            n.setOrganization(a.getOrganization());
            n.setRole(a.getRole());
            n.setStartDate(a.getStartDate());
            n.setEndDate(a.getEndDate());
            n.setIsCurrent(a.getIsCurrent());
            n.setDescription(a.getDescription());
            n.setDisplayOrder(a.getDisplayOrder());
            activityRepo.save(n);
        });

        referenceRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(r -> {
            CvReference n = new CvReference();
            n.setCvId(targetCvId);
            n.setFullName(r.getFullName());
            n.setPosition(r.getPosition());
            n.setCompany(r.getCompany());
            n.setEmail(r.getEmail());
            n.setPhone(r.getPhone());
            n.setRelationship(r.getRelationship());
            n.setDisplayOrder(r.getDisplayOrder());
            referenceRepo.save(n);
        });
    }
}
