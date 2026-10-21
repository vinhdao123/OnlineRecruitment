package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.CvTemplateVersion;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CvService {

    private final CvRepository cvRepository;
    private final CandidateRepository candidateRepository;
    private final CvTemplateVersionRepository templateVersionRepository;

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
                        cv.getAvatarUrl() // Truyền thêm avatarUrl
                ))
                .toList();
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
    public Cv getCvDetail(Long cvId, Long userId) {
        Candidate candidate = getCandidate(userId);
        return cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV hoặc bạn không có quyền truy cập"));
    }

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
        cv.setIsDefault(isFirstCv); // CV đầu tiên tự động làm mặc định
        cv.setAvatarUrl(req.getAvatarUrl());

        cvRepository.save(cv);
    }

    @Transactional
    public void updateCv(Long cvId, UpdateCvRequest req, Long userId) {
        Candidate candidate = getCandidate(userId);

        Cv cv = cvRepository.findByIdAndCandidateId(cvId, candidate.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CV"));
        cv.setAvatarUrl(req.getAvatarUrl());

        if (!"ACTIVE".equals(cv.getStatus())) {
            throw new IllegalArgumentException("Không thể sửa CV đã bị vô hiệu hóa");
        }

        cv.setTitle(req.getTitle());
        cv.setProfessionalTitle(req.getProfessionalTitle());
        cv.setSummary(req.getSummary());
        cv.setAccentColor(req.getAccentColor());
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
            return; // Đã là mặc định rồi
        }

        // 1. Bỏ mặc định của CV cũ (nếu có)
        cvRepository.findByCandidateIdAndIsDefaultTrueAndStatus(candidate.getId(), "ACTIVE")
                .ifPresent(oldDefault -> {
                    oldDefault.setIsDefault(false);
                    cvRepository.save(oldDefault);
                    // BẮT BUỘC: Ép Hibernate đẩy lệnh UPDATE is_default = 0 xuống DB ngay lập tức
                    cvRepository.flush();
                });

        // 2. Set mặc định CV mới
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

    private Candidate getCandidate(Long userId) {
        return candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có hồ sơ ứng viên"));
    }

    private final OnlineRecruitment.OnlineRecruitment.repository.candidate.CvWorkExperienceRepository workRepo;

    @Transactional(readOnly = true)
    public List<OnlineRecruitment.OnlineRecruitment.entity.CvWorkExperience> getWorkExperiences(Long cvId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership
        return workRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addWorkExperience(Long cvId, WorkExperienceRequest req, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        // 1. Kiểm tra ngày bắt đầu không được ở tương lai
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }

        // 2. Kiểm tra logic ngày tháng
        if (Boolean.TRUE.equals(req.getIsCurrent())) {
            req.setEndDate(null);
        } else {
            if (req.getEndDate() != null && req.getStartDate() != null) {
                if (req.getStartDate().isAfter(req.getEndDate())) {
                    throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
                }
            }
        }

        // 3. Lưu dữ liệu

        OnlineRecruitment.OnlineRecruitment.entity.CvWorkExperience work = new OnlineRecruitment.OnlineRecruitment.entity.CvWorkExperience();
        work.setCvId(cvId);
        work.setCompanyName(req.getCompanyName());
        work.setJobTitle(req.getJobTitle());
        work.setLocation(req.getLocation());
        work.setStartDate(req.getStartDate());

        // Gán trực tiếp từ req, không tự ý đổi thành null nếu có giá trị
        work.setEndDate(req.getEndDate());
        work.setIsCurrent(req.getIsCurrent() != null ? req.getIsCurrent() : false);

        work.setDescription(req.getDescription());
        work.setDisplayOrder(workRepo.findMaxDisplayOrderByCvId(cvId) + 1);

        workRepo.save(work);
    }


    @Transactional
    public void deleteWorkExperience(Long cvId, Long workId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        OnlineRecruitment.OnlineRecruitment.entity.CvWorkExperience work = workRepo.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kinh nghiệm làm việc"));

        if (!work.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }

        workRepo.delete(work);
    }

    private final OnlineRecruitment.OnlineRecruitment.repository.candidate.CvEducationRepository eduRepo;

    @Transactional(readOnly = true)
    public List<OnlineRecruitment.OnlineRecruitment.entity.CvEducation> getEducations(Long cvId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership
        return eduRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addEducation(Long cvId, EducationRequest req, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        // 1. Kiểm tra logic ngày tháng
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            }
        }

        // 2. Xử lý ghép Xếp loại và GPA
        String finalGrade = "";
        if (req.getAcademicRank() != null && !req.getAcademicRank().isEmpty()) {
            finalGrade = req.getAcademicRank();
            if (req.getGpa() != null) {
                finalGrade += " (GPA: " + req.getGpa() + ")";
            }
        } else if (req.getGpa() != null) {
            finalGrade = "GPA: " + req.getGpa();
        }

        // 3. Lưu dữ liệu
        OnlineRecruitment.OnlineRecruitment.entity.CvEducation edu = new OnlineRecruitment.OnlineRecruitment.entity.CvEducation();
        edu.setCvId(cvId);
        edu.setInstitution(req.getInstitution());
        edu.setDegree(req.getDegree());
        edu.setFieldOfStudy(req.getFieldOfStudy());
        edu.setStartDate(req.getStartDate());
        edu.setEndDate(req.getEndDate());
        edu.setGrade(finalGrade); // Lưu chuỗi đã ghép
        edu.setDescription(req.getDescription());
        edu.setDisplayOrder(eduRepo.findMaxDisplayOrderByCvId(cvId) + 1);

        eduRepo.save(edu);
    }

    @Transactional
    public void deleteEducation(Long cvId, Long eduId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        OnlineRecruitment.OnlineRecruitment.entity.CvEducation edu = eduRepo.findById(eduId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học vấn"));

        if (!edu.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }

        eduRepo.delete(edu);
    }
    private final OnlineRecruitment.OnlineRecruitment.repository.candidate.CvSkillRepository cvSkillRepo;
    private final OnlineRecruitment.OnlineRecruitment.repository.candidate.SkillRepository skillRepo;

    @Transactional(readOnly = true)
    public List<OnlineRecruitment.OnlineRecruitment.entity.CvSkill> getSkills(Long cvId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership
        return cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional(readOnly = true)
    public List<OnlineRecruitment.OnlineRecruitment.entity.Skill> getAllAvailableSkills() {
        return skillRepo.findByStatusOrderByNameAsc("ACTIVE");
    }

    @Transactional
    public void addSkill(Long cvId, SkillRequest req, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        // 1. Kiểm tra kỹ năng có tồn tại và active không
        OnlineRecruitment.OnlineRecruitment.entity.Skill skill = skillRepo.findById(req.getSkillId())
                .orElseThrow(() -> new IllegalArgumentException("Kỹ năng không tồn tại"));
        if (!"ACTIVE".equals(skill.getStatus())) {
            throw new IllegalArgumentException("Kỹ năng này đã bị vô hiệu hóa");
        }

        // 2. Kiểm tra xem CV đã có kỹ năng này chưa (tránh lỗi trùng khóa chính)
        OnlineRecruitment.OnlineRecruitment.entity.CvSkillId id = new OnlineRecruitment.OnlineRecruitment.entity.CvSkillId(cvId, req.getSkillId());
        if (cvSkillRepo.existsById(id)) {
            throw new IllegalArgumentException("Kỹ năng này đã có trong CV của bạn");
        }

        // 3. Lưu dữ liệu
        OnlineRecruitment.OnlineRecruitment.entity.CvSkill cvSkill = new OnlineRecruitment.OnlineRecruitment.entity.CvSkill();
        cvSkill.setId(id);
        cvSkill.setProficiency(req.getProficiency());
        cvSkill.setDisplayOrder(cvSkillRepo.findMaxDisplayOrderByCvId(cvId) + 1);

        cvSkillRepo.save(cvSkill);
    }

    @Transactional
    public void deleteSkill(Long cvId, Long skillId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        OnlineRecruitment.OnlineRecruitment.entity.CvSkillId id = new OnlineRecruitment.OnlineRecruitment.entity.CvSkillId(cvId, skillId);
        if (!cvSkillRepo.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy kỹ năng này trong CV");
        }

        cvSkillRepo.deleteById(id);
    }
    private final OnlineRecruitment.OnlineRecruitment.repository.candidate.CvProjectRepository projRepo;

    @Transactional(readOnly = true)
    public List<OnlineRecruitment.OnlineRecruitment.entity.CvProject> getProjects(Long cvId, Long userId) {
        getCvDetail(cvId, userId); // Check ownership
        return projRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addProject(Long cvId, ProjectRequest req, Long userId) {
        getCvDetail(cvId, userId); // Check ownership

        // 1. Kiểm tra logic ngày tháng
        if (req.getStartDate() != null && req.getStartDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (req.getStartDate() != null && req.getEndDate() != null) {
            if (req.getStartDate().isAfter(req.getEndDate())) {
                throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
            }
        }

        // 2. Lưu dữ liệu
        OnlineRecruitment.OnlineRecruitment.entity.CvProject project = new OnlineRecruitment.OnlineRecruitment.entity.CvProject();
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
        getCvDetail(cvId, userId); // Check ownership

        OnlineRecruitment.OnlineRecruitment.entity.CvProject project = projRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dự án"));

        if (!project.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }

        projRepo.delete(project);
    }
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getFullCvForPreview(Long cvId, Long userId) {
        OnlineRecruitment.OnlineRecruitment.entity.Cv cv = getCvDetail(cvId, userId);

        java.util.Map<String, Object> model = new java.util.HashMap<>();
        model.put("cv", cv);
        model.put("works", getWorkExperiences(cvId, userId));
        model.put("educations", getEducations(cvId, userId));
        model.put("skills", getSkills(cvId, userId));
        model.put("projects", getProjects(cvId, userId));

        return model;
    }

    @Transactional(readOnly = true)
    public java.util.Map<Long, java.util.Map<String, Object>> getCvListWithDetails(Long userId) {
        OnlineRecruitment.OnlineRecruitment.entity.Candidate candidate = getCandidate(userId);

        // Lấy danh sách CV cơ bản
        java.util.List<OnlineRecruitment.OnlineRecruitment.entity.Cv> cvs =
                cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");

        // Dùng Map để chứa dữ liệu chi tiết của từng CV (Key: cvId)
        java.util.Map<Long, java.util.Map<String, Object>> cvDataMap = new java.util.HashMap<>();

        for (OnlineRecruitment.OnlineRecruitment.entity.Cv cv : cvs) {
            java.util.Map<String, Object> detail = new java.util.HashMap<>();
            detail.put("cv", cv);
            // Lấy dữ liệu con (tái sử dụng các hàm đã viết ở GĐ3)
            detail.put("works", getWorkExperiences(cv.getId(), userId));
            detail.put("educations", getEducations(cv.getId(), userId));
            detail.put("skills", getSkills(cv.getId(), userId));
            detail.put("projects", getProjects(cv.getId(), userId));

            cvDataMap.put(cv.getId(), detail);
        }

        return cvDataMap;
    }

}