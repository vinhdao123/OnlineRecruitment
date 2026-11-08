package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CvService {

    private final CvRepository cvRepository;
    private final CandidateRepository candidateRepository;
    private final CvTemplateVersionRepository templateVersionRepository;
    private final CvWorkExperienceRepository workRepo;
    private final CvEducationRepository eduRepo;
    private final CvProjectRepository projRepo;
    private final CvSkillRepository cvSkillRepo;
    private final SkillRepository skillRepo;
    private final CvCertificateRepository certRepo;
    private final CvAwardRepository awardRepo;
    private final CvActivityRepository activityRepo;
    private final CvReferenceRepository referenceRepo;
    private final DataSource dataSource;

    @PostConstruct
    public void initCvTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. cv_projects
            stmt.execute("IF OBJECT_ID('dbo.cv_projects', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_projects (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  role NVARCHAR(255) NULL," +
                    "  start_date DATE NULL," +
                    "  end_date DATE NULL," +
                    "  project_url NVARCHAR(1000) NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  technologies NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_projects_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 2. cv_certificates
            stmt.execute("IF OBJECT_ID('dbo.cv_certificates', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_certificates (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  issuing_org NVARCHAR(255) NULL," +
                    "  issue_date DATE NULL," +
                    "  expiry_date DATE NULL," +
                    "  credential_id NVARCHAR(255) NULL," +
                    "  credential_url NVARCHAR(1000) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_certificates_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 3. cv_awards
            stmt.execute("IF OBJECT_ID('dbo.cv_awards', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_awards (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  name NVARCHAR(255) NOT NULL," +
                    "  issuer NVARCHAR(255) NULL," +
                    "  award_date DATE NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_awards_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 4. cv_activities
            stmt.execute("IF OBJECT_ID('dbo.cv_activities', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_activities (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  organization NVARCHAR(255) NOT NULL," +
                    "  role NVARCHAR(255) NULL," +
                    "  start_date DATE NULL," +
                    "  end_date DATE NULL," +
                    "  description NVARCHAR(MAX) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_activities_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 5. cv_references
            stmt.execute("IF OBJECT_ID('dbo.cv_references', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_references (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  cv_id BIGINT NOT NULL," +
                    "  full_name NVARCHAR(255) NOT NULL," +
                    "  position NVARCHAR(255) NULL," +
                    "  organization NVARCHAR(255) NULL," +
                    "  email NVARCHAR(255) NULL," +
                    "  phone NVARCHAR(30) NULL," +
                    "  display_order INT NOT NULL DEFAULT 0," +
                    "  CONSTRAINT FK_cv_references_cv FOREIGN KEY (cv_id) REFERENCES dbo.cv(id) ON DELETE CASCADE" +
                    ")");

            // 6. cv_templates & cv_template_versions
            stmt.execute("IF OBJECT_ID('dbo.cv_templates', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_templates (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  category_id BIGINT NOT NULL DEFAULT 1," +
                    "  name NVARCHAR(150) NOT NULL," +
                    "  code NVARCHAR(100) NOT NULL UNIQUE," +
                    "  status NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE'" +
                    ")");

            stmt.execute("IF OBJECT_ID('dbo.cv_template_versions', 'U') IS NULL " +
                    "CREATE TABLE dbo.cv_template_versions (" +
                    "  id BIGINT IDENTITY(1,1) PRIMARY KEY," +
                    "  template_id BIGINT NOT NULL," +
                    "  version_number INT NOT NULL DEFAULT 1," +
                    "  layout_definition NVARCHAR(MAX) NOT NULL DEFAULT ''," +
                    "  is_published BIT NOT NULL DEFAULT 1," +
                    "  CONSTRAINT FK_cv_template_versions_template FOREIGN KEY (template_id) REFERENCES dbo.cv_templates(id) ON DELETE CASCADE" +
                    ")");

            // Seed 5 templates if not exists
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'STANDARD') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Tiêu chuẩn', 'STANDARD', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'MINIMALIST') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Tiêu chuẩn (Ít kinh nghiệm)', 'MINIMALIST', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'CREATIVE') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Thanh lịch', 'CREATIVE', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'MODERN') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Hiện đại (2 cột)', 'MODERN', 'ACTIVE')");
            stmt.execute("IF NOT EXISTS (SELECT 1 FROM dbo.cv_templates WHERE code = 'ATS_FRIENDLY') " +
                    "INSERT INTO dbo.cv_templates (category_id, name, code, status) VALUES (1, N'Chuẩn ATS', 'ATS_FRIENDLY', 'ACTIVE')");

            // Seed versions
            stmt.execute("INSERT INTO dbo.cv_template_versions (template_id, version_number, layout_definition, is_published) " +
                    "SELECT t.id, 1, t.code, 1 FROM dbo.cv_templates t " +
                    "WHERE NOT EXISTS (SELECT 1 FROM dbo.cv_template_versions v WHERE v.template_id = t.id AND v.version_number = 1)");

            log.info("Initialized CV extra tables and 5 templates successfully.");
        } catch (Exception e) {
            log.warn("Notice while initializing CV tables: {}", e.getMessage());
        }
    }

    // ==========================================
    // 1. QUẢN LÝ DANH SÁCH & CHI TIẾT CV
    // ==========================================

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvSummaryVm> listMyCvs(Long userId) {
        Candidate candidate = getCandidate(userId);
        return cvRepository
                .findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE")
                .stream()
                .map(cv -> new CvSummaryVm(
                        cv.getId(),
                        cv.getTitle(),
                        cv.getProfessionalTitle(),
                        cv.getStatus(),
                        cv.getIsDefault(),
                        cv.getUpdatedAt(),
                        cv.getTemplateVersion() != null ? cv.getTemplateVersion().getTemplate().getName() : "N/A",
                        cv.getAvatarUrl(),
                        cv.getIsUploaded() != null ? cv.getIsUploaded() : false,
                        cv.getFileUrl()
                ))
                .toList();
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<Long, Map<String, Object>> getCvListWithDetails(Long userId) {
        Candidate candidate = getCandidate(userId);
        List<Cv> cvs = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");

        Map<Long, Map<String, Object>> cvDataMap = new java.util.HashMap<>();
        for (Cv cv : cvs) {
            Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("cv", cv);
            detail.put("works", getWorkExperiences(cv.getId(), userId));
            detail.put("educations", getEducations(cv.getId(), userId));
            detail.put("skills", getSkills(cv.getId(), userId));
            detail.put("projects", getProjects(cv.getId(), userId));
            cvDataMap.put(cv.getId(), detail);
        }
        return cvDataMap;
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Page<CvSummaryVm> listMyCvsPaged(Long userId, String keyword, int page, int size) {
        Candidate candidate = getCandidate(userId);
        Pageable pageable = PageRequest.of(Math.max(0, page), size > 0 ? size : 6);
        Page<Cv> cvPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            cvPage = cvRepository.findByCandidateIdAndStatusAndTitleContainingIgnoreCaseOrderByIsDefaultDescUpdatedAtDesc(
                    candidate.getId(), "ACTIVE", keyword.trim(), pageable);
        } else {
            cvPage = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
                    candidate.getId(), "ACTIVE", pageable);
        }
        return cvPage.map(cv -> new CvSummaryVm(
                cv.getId(),
                cv.getTitle(),
                cv.getProfessionalTitle(),
                cv.getStatus(),
                cv.getIsDefault(),
                cv.getUpdatedAt(),
                cv.getTemplateVersion() != null && cv.getTemplateVersion().getTemplate() != null
                        ? cv.getTemplateVersion().getTemplate().getName() : "N/A",
                cv.getAvatarUrl(),
                cv.getIsUploaded() != null ? cv.getIsUploaded() : false,
                cv.getFileUrl()
        ));
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<Long, Map<String, Object>> getCvListWithDetailsForCvs(List<CvSummaryVm> cvList, Long userId) {
        Map<Long, Map<String, Object>> map = new java.util.HashMap<>();
        if (cvList == null || cvList.isEmpty()) {
            return map;
        }
        for (CvSummaryVm cvSummary : cvList) {
            Cv cv = cvRepository.findById(cvSummary.getId()).orElse(null);
            if (cv != null) {
                Map<String, Object> detail = new java.util.HashMap<>();
                detail.put("cv", cv);
                detail.put("works", getWorkExperiences(cv.getId(), userId));
                detail.put("educations", getEducations(cv.getId(), userId));
                detail.put("skills", getSkills(cv.getId(), userId));
                detail.put("projects", getProjects(cv.getId(), userId));

                String templateCode = "STANDARD";
                String templateName = "Tiêu chuẩn";
                if (cv.getTemplateVersion() != null && cv.getTemplateVersion().getTemplate() != null) {
                    templateCode = cv.getTemplateVersion().getTemplate().getCode();
                    templateName = cv.getTemplateVersion().getTemplate().getName();
                } else if (cv.getTemplateVersionId() != null) {
                    CvTemplateVersion tv = templateVersionRepository.findById(cv.getTemplateVersionId()).orElse(null);
                    if (tv != null && tv.getTemplate() != null) {
                        templateCode = tv.getTemplate().getCode();
                        templateName = tv.getTemplate().getName();
                    } else if (tv != null && tv.getLayoutDefinition() != null) {
                        templateCode = tv.getLayoutDefinition();
                    }
                }
                detail.put("templateCode", templateCode != null ? templateCode.toUpperCase() : "STANDARD");
                detail.put("templateName", templateName);

                map.put(cv.getId(), detail);
            }
        }
        return map;
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Cv getCvDetail(Long cvId, Long userId) {
        Candidate candidate = getCandidate(userId);
        return cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV hoặc bạn không có quyền truy cập"));
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvTemplateOptionVm> getAvailableTemplates() {
        return templateVersionRepository.findByIsPublishedTrue().stream()
                .map(v -> new CvTemplateOptionVm(
                        v.getId(),
                        v.getTemplate() != null ? v.getTemplate().getName() : "Template",
                        v.getVersionNumber()
                ))
                .toList();
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Map<String, Object> getFullCvForPreview(Long cvId, Long userId) {
        Cv cv = getCvDetail(cvId, userId);
        Candidate candidate = getCandidate(userId);

        String templateCode = "STANDARD";
        String templateName = "Tiêu chuẩn";
        if (cv.getTemplateVersion() != null && cv.getTemplateVersion().getTemplate() != null) {
            templateCode = cv.getTemplateVersion().getTemplate().getCode();
            templateName = cv.getTemplateVersion().getTemplate().getName();
        } else if (cv.getTemplateVersionId() != null) {
            CvTemplateVersion tv = templateVersionRepository.findById(cv.getTemplateVersionId()).orElse(null);
            if (tv != null && tv.getTemplate() != null) {
                templateCode = tv.getTemplate().getCode();
                templateName = tv.getTemplate().getName();
            } else if (tv != null && tv.getLayoutDefinition() != null) {
                templateCode = tv.getLayoutDefinition();
            }
        }
        if (templateCode == null || templateCode.isBlank()) {
            templateCode = "STANDARD";
        }
        templateCode = templateCode.trim().toUpperCase();

        Map<String, Object> model = new java.util.HashMap<>();
        model.put("cv", cv);
        model.put("templateCode", templateCode);
        model.put("templateName", templateName);
        model.put("accentColor", cv.getAccentColor() != null ? cv.getAccentColor() : "#2563EB");
        model.put("candidate", candidate);
        model.put("candidateUser", candidate != null ? candidate.getUser() : null);
        model.put("works", getWorkExperiences(cvId, userId));
        model.put("educations", getEducations(cvId, userId));
        model.put("skills", getSkills(cvId, userId));
        model.put("projects", getProjects(cvId, userId));
        model.put("certificates", getCertificates(cvId, userId));
        model.put("awards", getAwards(cvId, userId));
        model.put("activities", getActivities(cvId, userId));
        model.put("references", getReferences(cvId, userId));
        return model;
    }

    // ==========================================
    // 2. TẠO & CẬP NHẬT CV
    // ==========================================

    @Transactional
    public void createCv(CreateCvRequest req, Long userId) {
        Candidate candidate = getCandidate(userId);
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
    }

    @Transactional
    public Long createCvAndReturnId(CreateCvRequest req, Long userId) {
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);
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
        Candidate candidate = getCandidate(userId);

        cvRepository.findByIdAndCandidateId(sourceCvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV nguồn"));

        workRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(work -> {
            CvWorkExperience newWork = new CvWorkExperience();
            newWork.setCvId(targetCvId);
            newWork.setCompanyName(work.getCompanyName());
            newWork.setJobTitle(work.getJobTitle());
            newWork.setLocation(work.getLocation());
            newWork.setStartDate(work.getStartDate());
            newWork.setEndDate(work.getEndDate());
            newWork.setIsCurrent(work.getIsCurrent());
            newWork.setDescription(work.getDescription());
            newWork.setDisplayOrder(work.getDisplayOrder());
            workRepo.save(newWork);
        });

        eduRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(edu -> {
            CvEducation newEdu = new CvEducation();
            newEdu.setCvId(targetCvId);
            newEdu.setInstitution(edu.getInstitution());
            newEdu.setDegree(edu.getDegree());
            newEdu.setFieldOfStudy(edu.getFieldOfStudy());
            newEdu.setStartDate(edu.getStartDate());
            newEdu.setEndDate(edu.getEndDate());
            newEdu.setGrade(edu.getGrade());
            newEdu.setDescription(edu.getDescription());
            newEdu.setDisplayOrder(edu.getDisplayOrder());
            eduRepo.save(newEdu);
        });

        projRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(proj -> {
            CvProject newProj = new CvProject();
            newProj.setCvId(targetCvId);
            newProj.setName(proj.getName());
            newProj.setRole(proj.getRole());
            newProj.setStartDate(proj.getStartDate());
            newProj.setEndDate(proj.getEndDate());
            newProj.setProjectUrl(proj.getProjectUrl());
            newProj.setDescription(proj.getDescription());
            newProj.setTechnologies(proj.getTechnologies());
            newProj.setDisplayOrder(proj.getDisplayOrder());
            projRepo.save(newProj);
        });

        certRepo.findByCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(c -> {
            CvCertificate n = new CvCertificate();
            n.setCvId(targetCvId);
            n.setName(c.getName());
            n.setIssuer(c.getIssuer());
            n.setIssueDate(c.getIssueDate());
            n.setExpiryDate(c.getExpiryDate());
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
        cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(sourceCvId).forEach(skill -> {
            CvSkill newSkill = new CvSkill();
            CvSkillId newId = new CvSkillId(targetCvId, skill.getId().getSkillId());
            newSkill.setId(newId);
            newSkill.setProficiency(skill.getProficiency());
            newSkill.setDisplayOrder(skill.getDisplayOrder());
            cvSkillRepo.save(newSkill);
        });
    }

    // ==========================================
    // 4. CÁC HÀM QUẢN LÝ NỘI DUNG CV (WORK, EDU, SKILL, PROJECT)
    // ==========================================

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvWorkExperience> getWorkExperiences(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        return workRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addWorkExperience(Long cvId, WorkExperienceRequest req, Long userId) {
        getCvDetail(cvId, userId);
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            }
        }
        CvWorkExperience work = new CvWorkExperience();
        work.setCvId(cvId);
        work.setCompanyName(req.getCompanyName());
        work.setJobTitle(req.getJobTitle());
        work.setLocation(req.getLocation());
        work.setStartDate(req.getStartDate());
        work.setEndDate(req.getEndDate());
        work.setIsCurrent(req.getIsCurrent() != null ? req.getIsCurrent() : false);
        work.setDescription(req.getDescription());
        work.setDisplayOrder(workRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        workRepo.save(work);
    }

    @Transactional
    public void deleteWorkExperience(Long cvId, Long workId, Long userId) {
        getCvDetail(cvId, userId);
        CvWorkExperience work = workRepo.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kinh nghiệm làm việc"));
        if (!work.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        workRepo.delete(work);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvEducation> getEducations(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        return eduRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addEducation(Long cvId, EducationRequest req, Long userId) {
        getCvDetail(cvId, userId);
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            }
        }
        String finalGrade = "";
        if (req.getAcademicRank() != null && !req.getAcademicRank().isEmpty()) {
            finalGrade = req.getAcademicRank();
            if (req.getGpa() != null) {
                finalGrade += " (GPA: " + req.getGpa() + ")";
            }
        } else if (req.getGpa() != null) {
            finalGrade = "GPA: " + req.getGpa();
        }

        CvEducation edu = new CvEducation();
        edu.setCvId(cvId);
        edu.setInstitution(req.getInstitution());
        edu.setDegree(req.getDegree());
        edu.setFieldOfStudy(req.getFieldOfStudy());
        edu.setStartDate(req.getStartDate());
        edu.setEndDate(req.getEndDate());
        edu.setGrade(finalGrade);
        edu.setDescription(req.getDescription());
        edu.setDisplayOrder(eduRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        eduRepo.save(edu);
    }

    @Transactional
    public void deleteEducation(Long cvId, Long eduId, Long userId) {
        getCvDetail(cvId, userId);
        CvEducation edu = eduRepo.findById(eduId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học vấn"));
        if (!edu.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        eduRepo.delete(edu);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvSkill> getSkills(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        return cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<Skill> getAllAvailableSkills() {
        return skillRepo.findByStatusOrderByNameAsc("ACTIVE");
    }

    @Transactional
    public void addSkill(Long cvId, SkillRequest req, Long userId) {
        getCvDetail(cvId, userId);
        Skill skill = skillRepo.findById(req.getSkillId())
                .orElseThrow(() -> new IllegalArgumentException("Kỹ năng không tồn tại"));
        if (!"ACTIVE".equals(skill.getStatus())) {
            throw new IllegalArgumentException("Kỹ năng này đã bị vô hiệu hóa");
        }
        CvSkillId id = new CvSkillId(cvId, req.getSkillId());
        if (cvSkillRepo.existsById(id)) {
            throw new IllegalArgumentException("Kỹ năng này đã có trong CV của bạn");
        }
        CvSkill cvSkill = new CvSkill();
        cvSkill.setId(id);
        cvSkill.setProficiency(req.getProficiency());
        cvSkill.setDisplayOrder(cvSkillRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        cvSkillRepo.save(cvSkill);
    }

    @Transactional
    public void deleteSkill(Long cvId, Long skillId, Long userId) {
        getCvDetail(cvId, userId);
        CvSkillId id = new CvSkillId(cvId, skillId);
        if (!cvSkillRepo.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy kỹ năng này trong CV");
        }
        cvSkillRepo.deleteById(id);
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvProject> getProjects(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        try {
            return projRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch projects for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addProject(Long cvId, ProjectRequest req, Long userId) {
        getCvDetail(cvId, userId);
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            }
        }
        CvProject project = new CvProject();
        project.setCvId(cvId);
        project.setName(req.getName());
        project.setRole(req.getRole());
        project.setStartDate(req.getStartDate());
        project.setEndDate(req.getEndDate());
        project.setProjectUrl(req.getProjectUrl());
        project.setDescription(req.getDescription());
        project.setTechnologies(req.getTechnologies());
        project.setDisplayOrder(projRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        projRepo.save(project);
    }

    @Transactional
    public void deleteProject(Long cvId, Long projectId, Long userId) {
        getCvDetail(cvId, userId);
        CvProject project = projRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dự án"));
        if (!project.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        projRepo.delete(project);
    }

    // ==========================================
    // 4b. CHỨNG CHỈ, GIẢI THƯỞNG, HOẠT ĐỘNG, NGƯỜI THAM CHIẾU
    // ==========================================

    private void validateDateRange(java.time.LocalDate start, java.time.LocalDate end, boolean startNotFuture) {
        if (startNotFuture && start != null && start.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
    }

    // ---- Chứng chỉ ----
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvCertificate> getCertificates(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        try {
            return certRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch certificates for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addCertificate(Long cvId, CertificateRequest req, Long userId) {
        getCvDetail(cvId, userId);
        if (req.getIssueDate() != null && req.getExpiryDate() != null && req.getIssueDate().isAfter(req.getExpiryDate())) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau hoặc bằng ngày cấp.");
        }
        CvCertificate c = new CvCertificate();
        c.setCvId(cvId);
        c.setName(req.getName());
        c.setIssuer(req.getIssuer());
        c.setIssueDate(req.getIssueDate());
        c.setExpiryDate(req.getExpiryDate());
        c.setCredentialUrl(req.getCredentialUrl());
        c.setDescription(req.getDescription());
        c.setDisplayOrder(certRepo.findMaxDisplayOrderByCvId(cvId) + 1);
        certRepo.save(c);
    }

    @Transactional
    public void deleteCertificate(Long cvId, Long certId, Long userId) {
        getCvDetail(cvId, userId);
        CvCertificate c = certRepo.findById(certId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chứng chỉ"));
        if (!c.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        certRepo.delete(c);
    }

    // ---- Giải thưởng ----
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvAward> getAwards(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        try {
            return awardRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch awards for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addAward(Long cvId, AwardRequest req, Long userId) {
        getCvDetail(cvId, userId);
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
        getCvDetail(cvId, userId);
        CvAward a = awardRepo.findById(awardId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giải thưởng"));
        if (!a.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        awardRepo.delete(a);
    }

    // ---- Hoạt động ngoại khóa ----
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvActivity> getActivities(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        try {
            return activityRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch activities for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addActivity(Long cvId, ActivityRequest req, Long userId) {
        getCvDetail(cvId, userId);
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
        getCvDetail(cvId, userId);
        CvActivity a = activityRepo.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hoạt động"));
        if (!a.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        activityRepo.delete(a);
    }

    // ---- Người tham chiếu ----
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public List<CvReference> getReferences(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        try {
            return referenceRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch references for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addReference(Long cvId, ReferenceRequest req, Long userId) {
        getCvDetail(cvId, userId);
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
        getCvDetail(cvId, userId);
        CvReference r = referenceRepo.findById(referenceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người tham chiếu"));
        if (!r.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        referenceRepo.delete(r);
    }

    // ==========================================
    // 4c. BỘ TÙY BIẾN MẪU CV (CUSTOMIZER)
    // ==========================================

    private static final java.util.Set<String> CUSTOMIZER_TEMPLATE_CODES =
            java.util.Set.of("STANDARD", "MINIMALIST", "CREATIVE", "MODERN", "ATS_FRIENDLY");
    private static final java.util.regex.Pattern HEX_COLOR = java.util.regex.Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /**
     * Dựng dữ liệu xem trước cho bộ tùy biến mẫu CV.
     *
     * @param selectedCvId CV cần xem trước; null = CV mặc định (nếu có), 0 = dùng dữ liệu mẫu
     * @param templateCode mã mẫu (STANDARD, MINIMALIST, CREATIVE, MODERN, ATS_FRIENDLY)
     * @param fontFamily   font tùy chọn (null = mặc định của mẫu)
     * @param accentColor  màu chủ đạo dạng #RRGGBB (null = lấy màu của CV hoặc mặc định)
     * @param avatarUrl    ảnh đại diện tùy chọn (null = dùng ảnh của CV / ảnh mẫu)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPreviewModelForCustomizer(Long selectedCvId, String templateCode, String fontFamily,
                                                            String accentColor, String avatarUrl, Long userId) {
        Candidate candidate = getCandidate(userId);
        List<Cv> myCvs = cvRepository
                .findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");

        Cv selected = null;
        if (selectedCvId == null) {
            selected = myCvs.isEmpty() ? null : myCvs.get(0);
        } else if (selectedCvId > 0) {
            selected = myCvs.stream().filter(c -> c.getId().equals(selectedCvId)).findFirst().orElse(null);
        }

        String code = templateCode != null ? templateCode.trim().toUpperCase() : "";
        if (!CUSTOMIZER_TEMPLATE_CODES.contains(code)) {
            code = "STANDARD";
        }

        String color;
        if (accentColor != null && HEX_COLOR.matcher(accentColor).matches()) {
            color = accentColor;
        } else if (selected != null && selected.getAccentColor() != null && HEX_COLOR.matcher(selected.getAccentColor()).matches()) {
            color = selected.getAccentColor();
        } else {
            color = "#2563EB";
        }

        Map<String, Object> model = new java.util.HashMap<>();
        model.put("cvOptions", myCvs);
        model.put("previewCv", selected);
        model.put("isSample", selected == null);
        model.put("selectedCvId", selected != null ? selected.getId() : 0L);
        model.put("templateCode", code);
        model.put("accentColor", color);
        model.put("fontFamily", fontFamily);
        model.put("avatarUrl", avatarUrl != null && !avatarUrl.isBlank()
                ? avatarUrl
                : (selected != null ? selected.getAvatarUrl() : null));

        if (selected != null) {
            Long id = selected.getId();
            model.put("works", workRepo.findByCvIdOrderByDisplayOrderAsc(id));
            model.put("educations", eduRepo.findByCvIdOrderByDisplayOrderAsc(id));
            model.put("skills", cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(id));
            model.put("projects", projRepo.findByCvIdOrderByDisplayOrderAsc(id));
        } else {
            model.put("works", List.of());
            model.put("educations", List.of());
            model.put("skills", List.of());
            model.put("projects", List.of());
        }
        return model;
    }
    // ==========================================
    // 5. HÀM BỔ TRỢ
    // ==========================================

    private Candidate getCandidate(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ ứng viên"));
    }
}