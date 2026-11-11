package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiệp vụ lõi của CV: danh sách, chi tiết, tạo, sửa, đặt mặc định, xoá, nhân bản, upload, đổi tên.
 *
 * Các phần khác đã được tách ra:
 * - Tiểu mục (kinh nghiệm, học vấn, kỹ năng, dự án): {@link CvSectionService}
 * - Tiểu mục bổ sung (chứng chỉ, giải thưởng, hoạt động, tham chiếu): {@link CvExtraSectionService}
 * - Mẫu CV, gallery, customizer: {@link CvTemplateService}
 * - Lưu / tìm file: {@link CvFileStorageService}
 * - Kiểm tra quyền sở hữu: {@link CvOwnershipService}
 * - Khởi tạo bảng: {@link CvSchemaInitializer}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvService {

    private final CvRepository cvRepository;
    private final CvTemplateVersionRepository templateVersionRepository;
    private final CvOwnershipService ownershipService;
    private final CvSectionService sectionService;
    private final CvExtraSectionService extraSectionService;
    private final CvTemplateService templateService;

    // ==========================================
    // 1. QUẢN LÝ DANH SÁCH & CHI TIẾT CV
    // ==========================================

    private CvSummaryVm toSummary(Cv cv) {
        return new CvSummaryVm(
                cv.getId(),
                cv.getTitle(),
                cv.getProfessionalTitle(),
                cv.getStatus(),
                cv.getIsDefault(),
                cv.getUpdatedAt(),
                templateService.resolveTemplateInfo(cv).name(),
                cv.getAvatarUrl(),
                cv.getIsUploaded() != null ? cv.getIsUploaded() : false,
                cv.getFileUrl()
        );
    }

    /** Nạp tóm tắt thông tin cho danh sách CV bằng batch query (tránh N+1 query). */
    private Map<Long, Map<String, Object>> buildCardsDetail(List<Cv> cvs) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        if (cvs == null || cvs.isEmpty()) {
            return map;
        }
        List<Long> cvIds = cvs.stream().map(Cv::getId).toList();
        Map<Long, List<CvWorkExperience>> worksByCv = sectionService.getWorkExperiencesByCvIds(cvIds);
        Map<Long, List<CvEducation>> edusByCv = sectionService.getEducationsByCvIds(cvIds);
        Map<Long, List<CvSkill>> skillsByCv = sectionService.getSkillsByCvIds(cvIds);
        Map<Long, List<CvProject>> projsByCv = sectionService.getProjectsByCvIds(cvIds);

        for (Cv cv : cvs) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("cv", cv);
            detail.put("works", worksByCv.getOrDefault(cv.getId(), List.of()));
            detail.put("educations", edusByCv.getOrDefault(cv.getId(), List.of()));
            detail.put("skills", skillsByCv.getOrDefault(cv.getId(), List.of()));
            detail.put("projects", projsByCv.getOrDefault(cv.getId(), List.of()));
            CvTemplateService.TemplateInfo template = templateService.resolveTemplateInfo(cv);
            detail.put("templateCode", template.code());
            detail.put("templateName", template.name());
            map.put(cv.getId(), detail);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<CvSummaryVm> listMyCvs(Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        return cvRepository
                .findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE")
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getDefaultCvTitle(Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        return cvRepository.findByCandidateIdAndIsDefaultTrueAndStatus(candidate.getId(), "ACTIVE")
                .map(Cv::getTitle)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> getCvListWithDetails(Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        List<Cv> cvs = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");
        return buildCardsDetail(cvs);
    }

    @Transactional(readOnly = true)
    public Page<CvSummaryVm> listMyCvsPaged(Long userId, String keyword, int page, int size) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 6);
        Page<Cv> cvPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            cvPage = cvRepository.findByCandidateIdAndStatusAndTitleContainingIgnoreCaseOrderByIsDefaultDescUpdatedAtDesc(
                    candidate.getId(), "ACTIVE", keyword.trim(), pageable);
        } else {
            cvPage = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
                    candidate.getId(), "ACTIVE", pageable);
        }
        return cvPage.map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> getCvListWithDetailsForCvs(List<CvSummaryVm> cvList, Long userId) {
        if (cvList == null || cvList.isEmpty()) {
            return new HashMap<>();
        }
        Long candidateId = ownershipService.getCandidate(userId).getId();
        List<Long> cvIds = cvList.stream().map(CvSummaryVm::getId).toList();
        List<Cv> cvs = cvRepository.findByCandidateIdAndIdIn(candidateId, cvIds);
        return buildCardsDetail(cvs);
    }

    @Transactional(readOnly = true)
    public Cv getCvDetail(Long cvId, Long userId) {
        return ownershipService.getOwnedCv(cvId, userId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFullCvForPreview(Long cvId, Long userId) {
        Cv cv = getCvDetail(cvId, userId);
        Candidate candidate = ownershipService.getCandidate(userId);
        CvTemplateService.TemplateInfo template = templateService.resolveTemplateInfo(cv);

        Map<String, Object> model = new HashMap<>();
        model.put("cv", cv);
        model.put("templateCode", template.code());
        model.put("templateName", template.name());
        model.put("accentColor", cv.getAccentColor() != null ? cv.getAccentColor() : "#2563EB");
        model.put("candidate", candidate);
        model.put("candidateUser", candidate != null ? candidate.getUser() : null);
        model.put("works", sectionService.getWorkExperiences(cvId, userId));
        model.put("educations", sectionService.getEducations(cvId, userId));
        model.put("skills", sectionService.getSkills(cvId, userId));
        model.put("projects", sectionService.getProjects(cvId, userId));
        model.put("certificates", extraSectionService.getCertificates(cvId, userId));
        model.put("awards", extraSectionService.getAwards(cvId, userId));
        model.put("activities", extraSectionService.getActivities(cvId, userId));
        model.put("references", extraSectionService.getReferences(cvId, userId));
        return model;
    }

    // ==========================================
    // 2. TẠO & CẬP NHẬT CV
    // ==========================================

    @Transactional
    public void createCv(CreateCvRequest req, Long userId) {
        createCvAndReturnId(req, userId);
    }

    @Transactional
    public Long createCvAndReturnId(CreateCvRequest req, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        CvTemplateVersion template = templateVersionRepository.findById(req.getTemplateVersionId())
                .orElseThrow(() -> new IllegalArgumentException("Mẫu CV không hợp lệ"));

        if (!template.getIsPublished()) {
            throw new IllegalArgumentException("Mẫu CV này đã ngừng sử dụng");
        }

        boolean isFirstCv = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE").isEmpty();

        Cv cv = new Cv();
        cv.setCandidateId(candidate.getId());
        cv.setTemplateVersionId(template.getId());
        cv.setTitle(req.getTitle());
        cv.setProfessionalTitle(req.getProfessionalTitle());
        cv.setSummary(req.getSummary());
        cv.setAccentColor(req.getAccentColor());
        cv.setStatus("ACTIVE");
        cv.setIsDefault(isFirstCv);
        cv.setAvatarUrl(req.getAvatarUrl());

        cvRepository.save(cv);
        return cv.getId();
    }

    @Transactional
    public void updateCv(Long cvId, UpdateCvRequest req, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Cv cv = cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV"));

        if (!"ACTIVE".equals(cv.getStatus())) {
            throw new IllegalArgumentException("Không thể sửa CV đã bị vô hiệu hóa");
        }

        if (req.getTemplateVersionId() != null) {
            templateVersionRepository.findById(req.getTemplateVersionId())
                    .ifPresent(v -> {
                        cv.setTemplateVersionId(v.getId());
                        cv.setTemplateVersion(v);
                    });
        }

        cv.setTitle(req.getTitle());
        cv.setProfessionalTitle(req.getProfessionalTitle());
        cv.setSummary(req.getSummary());
        cv.setAccentColor(req.getAccentColor());
        if (req.getAvatarUrl() != null) {
            cv.setAvatarUrl(req.getAvatarUrl());
        }
        cv.setUpdatedAt(LocalDateTime.now());

        cvRepository.save(cv);
    }

    @Transactional
    public void setDefault(Long cvId, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Cv targetCv = cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV"));

        if (!"ACTIVE".equals(targetCv.getStatus())) {
            throw new IllegalArgumentException("CV đã vô hiệu hóa không thể đặt làm mặc định");
        }

        if (Boolean.TRUE.equals(targetCv.getIsDefault())) {
            return;
        }

        cvRepository.findByCandidateIdAndIsDefaultTrueAndStatus(candidate.getId(), "ACTIVE")
                .ifPresent(oldDefault -> {
                    oldDefault.setIsDefault(false);
                    cvRepository.save(oldDefault);
                    cvRepository.flush();
                });

        targetCv.setIsDefault(true);
        targetCv.setUpdatedAt(LocalDateTime.now());
        cvRepository.save(targetCv);
    }

    @Transactional
    public void softDelete(Long cvId, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Cv cv = cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV"));

        if (Boolean.TRUE.equals(cv.getIsDefault())) {
            throw new IllegalArgumentException("Không thể xóa CV đang là mặc định. Vui lòng đặt CV khác làm mặc định trước.");
        }

        cv.setStatus("INACTIVE");
        cv.setUpdatedAt(LocalDateTime.now());
        cvRepository.save(cv);
    }

    @Transactional
    public Long cloneCv(Long cvId, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Cv source = cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV"));
        if (!"ACTIVE".equals(source.getStatus())) {
            throw new IllegalArgumentException("Không thể nhân bản CV đã bị vô hiệu hóa");
        }
        if (Boolean.TRUE.equals(source.getIsUploaded())) {
            throw new IllegalArgumentException("Không thể nhân bản CV tải lên từ máy");
        }

        String newTitle = source.getTitle() + " (Bản sao)";
        if (newTitle.length() > 255) {
            newTitle = newTitle.substring(0, 255);
        }

        Cv copy = new Cv();
        copy.setCandidateId(candidate.getId());
        copy.setTemplateVersionId(source.getTemplateVersionId());
        copy.setTitle(newTitle);
        copy.setProfessionalTitle(source.getProfessionalTitle());
        copy.setSummary(source.getSummary());
        copy.setAccentColor(source.getAccentColor());
        copy.setStatus("ACTIVE");
        copy.setIsDefault(false);
        copy.setAvatarUrl(source.getAvatarUrl());
        copy.setContent(source.getContent());
        copy.setTemplate(source.getTemplate());
        cvRepository.save(copy);

        copyCvData(cvId, copy.getId(), userId);
        return copy.getId();
    }

    @Transactional
    public void uploadCvFile(String fileUrl, String title, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);
        Cv cv = new Cv();
        cv.setCandidateId(candidate.getId());
        cv.setTemplateVersionId(1L); // Mặc định template version 1
        cv.setTitle(title != null && !title.trim().isEmpty() ? title.trim() : "CV Upload từ file");
        cv.setProfessionalTitle("CV Tải lên");
        cv.setSummary("");
        cv.setAccentColor("#2563EB");
        cv.setStatus("ACTIVE");
        cv.setIsDefault(false);
        cv.setFileUrl(fileUrl);
        cv.setIsUploaded(true);
        cvRepository.save(cv);
    }

    @Transactional
    public void uploadCvFile(String fileUrl, Long userId) {
        uploadCvFile(fileUrl, "CV Upload từ file", userId);
    }

    @Transactional
    public void renameCv(Long cvId, String newTitle, Long userId) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên CV không được để trống!");
        }
        Cv cv = getCvDetail(cvId, userId);
        cv.setTitle(newTitle.trim());
        cvRepository.save(cv);
    }

    // ==========================================
    // 3. COPY DỮ LIỆU TỪ CV KHÁC
    // ==========================================

    @Transactional
    public void copyCvData(Long sourceCvId, Long targetCvId, Long userId) {
        Candidate candidate = ownershipService.getCandidate(userId);

        cvRepository.findByIdAndCandidateId(sourceCvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV nguồn"));

        sectionService.copySections(sourceCvId, targetCvId);
        extraSectionService.copySections(sourceCvId, targetCvId);
    }
}
