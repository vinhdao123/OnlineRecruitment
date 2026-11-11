package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Các tiểu mục chính của CV: Kinh nghiệm làm việc, Học vấn, Kỹ năng, Dự án.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CvSectionService {

    private final CvOwnershipService ownershipService;
    private final CvWorkExperienceRepository workRepo;
    private final CvEducationRepository eduRepo;
    private final CvProjectRepository projRepo;
    private final CvSkillRepository cvSkillRepo;
    private final SkillRepository skillRepo;

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && start.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu không được ở trong tương lai.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
    }

    // ---------- KINH NGHIỆM LÀM VIỆC ----------

    @Transactional(readOnly = true)
    public List<CvWorkExperience> getWorkExperiences(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        return workRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    // ---------- NẠP HÀNG LOẠT THEO DANH SÁCH CV ID (TRÁNH N+1 QUERY) ----------

    @Transactional(readOnly = true)
    public Map<Long, List<CvWorkExperience>> getWorkExperiencesByCvIds(Collection<Long> cvIds) {
        if (cvIds == null || cvIds.isEmpty()) return Collections.emptyMap();
        return workRepo.findByCvIdInOrderByDisplayOrderAsc(cvIds).stream()
                .collect(Collectors.groupingBy(CvWorkExperience::getCvId));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CvEducation>> getEducationsByCvIds(Collection<Long> cvIds) {
        if (cvIds == null || cvIds.isEmpty()) return Collections.emptyMap();
        return eduRepo.findByCvIdInOrderByDisplayOrderAsc(cvIds).stream()
                .collect(Collectors.groupingBy(CvEducation::getCvId));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CvSkill>> getSkillsByCvIds(Collection<Long> cvIds) {
        if (cvIds == null || cvIds.isEmpty()) return Collections.emptyMap();
        return cvSkillRepo.findByIdCvIdInOrderByDisplayOrderAsc(cvIds).stream()
                .collect(Collectors.groupingBy(s -> s.getId().getCvId()));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<CvProject>> getProjectsByCvIds(Collection<Long> cvIds) {
        if (cvIds == null || cvIds.isEmpty()) return Collections.emptyMap();
        try {
            return projRepo.findByCvIdInOrderByDisplayOrderAsc(cvIds).stream()
                    .collect(Collectors.groupingBy(CvProject::getCvId));
        } catch (Exception e) {
            log.warn("Cannot batch fetch projects: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Transactional
    public void addWorkExperience(Long cvId, WorkExperienceRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        validateDates(req.getStartDate(), req.getEndDate());
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
        ownershipService.getOwnedCv(cvId, userId);
        CvWorkExperience work = workRepo.findById(workId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kinh nghiệm làm việc"));
        if (!work.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        workRepo.delete(work);
    }

    // ---------- HỌC VẤN ----------

    @Transactional(readOnly = true)
    public List<CvEducation> getEducations(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        return eduRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional
    public void addEducation(Long cvId, EducationRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        validateDates(req.getStartDate(), req.getEndDate());
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
        ownershipService.getOwnedCv(cvId, userId);
        CvEducation edu = eduRepo.findById(eduId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học vấn"));
        if (!edu.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        eduRepo.delete(edu);
    }

    // ---------- KỸ NĂNG ----------

    @Transactional(readOnly = true)
    public List<CvSkill> getSkills(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        return cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(cvId);
    }

    @Transactional(readOnly = true)
    public List<Skill> getAllAvailableSkills() {
        return skillRepo.findByStatusOrderByNameAsc("ACTIVE");
    }

    @Transactional
    public void addSkill(Long cvId, SkillRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
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
        ownershipService.getOwnedCv(cvId, userId);
        CvSkillId id = new CvSkillId(cvId, skillId);
        if (!cvSkillRepo.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy kỹ năng này trong CV");
        }
        cvSkillRepo.deleteById(id);
    }

    // ---------- DỰ ÁN ----------

    @Transactional(readOnly = true)
    public List<CvProject> getProjects(Long cvId, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        try {
            return projRepo.findByCvIdOrderByDisplayOrderAsc(cvId);
        } catch (Exception e) {
            log.warn("Cannot fetch projects for cvId {}: {}", cvId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void addProject(Long cvId, ProjectRequest req, Long userId) {
        ownershipService.getOwnedCv(cvId, userId);
        validateDates(req.getStartDate(), req.getEndDate());
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
        ownershipService.getOwnedCv(cvId, userId);
        CvProject project = projRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dự án"));
        if (!project.getCvId().equals(cvId)) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        projRepo.delete(project);
    }

    // ---------- SAO CHÉP SANG CV KHÁC ----------

    /** Sao chép kinh nghiệm, học vấn, dự án, kỹ năng từ CV nguồn sang CV đích (đã kiểm tra quyền ở tầng gọi). */
    @Transactional
    public void copySections(Long sourceCvId, Long targetCvId) {
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
            newSkill.setId(new CvSkillId(targetCvId, skill.getId().getSkillId()));
            newSkill.setProficiency(skill.getProficiency());
            newSkill.setDisplayOrder(skill.getDisplayOrder());
            cvSkillRepo.save(newSkill);
        });
    }
}
