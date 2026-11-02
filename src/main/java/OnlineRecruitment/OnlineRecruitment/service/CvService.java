package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CvService {

    private final CvRepository cvRepository;
    private final CandidateRepository candidateRepository;
    private final CvTemplateVersionRepository templateVersionRepository;
    private final CvWorkExperienceRepository workRepo;
    private final CvEducationRepository eduRepo;
    private final CvProjectRepository projRepo;
    private final CvSkillRepository cvSkillRepo;
    private final SkillRepository skillRepo;

    // ==========================================
    // 1. QUẢN LÝ DANH SÁCH & CHI TIẾT CV
    // ==========================================

    @Transactional(readOnly = true)
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
                        cv.getIsUploaded() != null ? cv.getIsUploaded() : false
                ))
                .toList();
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public Cv getCvDetail(Long cvId, Long userId) {
        Candidate candidate = getCandidate(userId);
        return cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV hoặc bạn không có quyền truy cập"));
    }

    @Transactional(readOnly = true)
    public List<CvTemplateOptionVm> getAvailableTemplates() {
        return templateVersionRepository.findByIsPublishedTrue().stream()
                .map(v -> new CvTemplateOptionVm(
                        v.getId(),
                        v.getTemplate() != null ? v.getTemplate().getName() : "Template",
                        v.getVersionNumber()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFullCvForPreview(Long cvId, Long userId) {
        Cv cv = getCvDetail(cvId, userId);
        Map<String, Object> model = new java.util.HashMap<>();
        model.put("cv", cv);
        model.put("works", getWorkExperiences(cvId, userId));
        model.put("educations", getEducations(cvId, userId));
        model.put("skills", getSkills(cvId, userId));
        model.put("projects", getProjects(cvId, userId));
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
    public void uploadCvFile(String fileUrl, Long userId) {
        Candidate candidate = getCandidate(userId);
        Cv cv = new Cv();
        cv.setCandidateId(candidate.getId());
        cv.setTemplateVersionId(1L); // Mặc định template version 1
        cv.setTitle("CV Upload từ file"); // Có thể sửa để lấy tên file
        cv.setProfessionalTitle("");
        cv.setSummary("");
        cv.setAccentColor("#2563EB");
        cv.setStatus("ACTIVE");
        cv.setIsDefault(false);
        cv.setFileUrl(fileUrl);
        cv.setIsUploaded(true);
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<CvSkill> getSkills(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        return cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<CvProject> getProjects(Long cvId, Long userId) {
        getCvDetail(cvId, userId);
        return projRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
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
    // 5. HÀM BỔ TRỢ
    // ==========================================

    private Candidate getCandidate(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ ứng viên"));
    }
}