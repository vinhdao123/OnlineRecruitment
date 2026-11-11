package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.EducationRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.ProjectRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.SkillRequest;
import OnlineRecruitment.OnlineRecruitment.dto.candidate.WorkExperienceRequest;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvSectionService;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Các tiểu mục chính của CV: Kinh nghiệm làm việc, Học vấn, Kỹ năng, Dự án.
 */
@Controller
@RequestMapping("/candidate/cvs")
public class CvSectionController extends AbstractCvController {

    private final CvService cvService;
    private final CvSectionService sectionService;

    public CvSectionController(UserRepository userRepository,
                               CvService cvService,
                               CvSectionService sectionService) {
        super(userRepository);
        this.cvService = cvService;
        this.sectionService = sectionService;
    }

    // ---------- KINH NGHIỆM LÀM VIỆC ----------

    @GetMapping("/{id}/work-experiences")
    public String listWorkExp(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "works", userId -> sectionService.getWorkExperiences(id, userId),
                "workRequest", new WorkExperienceRequest(), "candidate/cv/work-experience", null);
    }

    @PostMapping("/{id}/work-experiences/add")
    public String addWorkExp(@PathVariable Long id, @Valid @ModelAttribute("workRequest") WorkExperienceRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("works", sectionService.getWorkExperiences(id, userId));
            return "candidate/cv/work-experience";
        }
        try {
            sectionService.addWorkExperience(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kinh nghiệm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/work-experiences";
    }

    @PostMapping("/{id}/work-experiences/{workId}/delete")
    public String deleteWorkExp(@PathVariable Long id, @PathVariable Long workId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> sectionService.deleteWorkExperience(id, workId, getUserId(auth)),
                "Đã xóa kinh nghiệm!", "/work-experiences");
    }

    // ---------- HỌC VẤN ----------

    @GetMapping("/{id}/educations")
    public String listEdu(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "educations", userId -> sectionService.getEducations(id, userId),
                "eduRequest", new EducationRequest(), "candidate/cv/education", null);
    }

    @PostMapping("/{id}/educations/add")
    public String addEdu(@PathVariable Long id, @Valid @ModelAttribute("eduRequest") EducationRequest req,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("educations", sectionService.getEducations(id, userId));
            return "candidate/cv/education";
        }
        try {
            sectionService.addEducation(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm học vấn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/educations";
    }

    @PostMapping("/{id}/educations/{eduId}/delete")
    public String deleteEdu(@PathVariable Long id, @PathVariable Long eduId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> sectionService.deleteEducation(id, eduId, getUserId(auth)),
                "Đã xóa học vấn!", "/educations");
    }

    // ---------- KỸ NĂNG ----------

    @GetMapping("/{id}/skills")
    public String listSkills(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "cvSkills", userId -> sectionService.getSkills(id, userId),
                "skillRequest", new SkillRequest(), "candidate/cv/skills",
                () -> model.addAttribute("allSkills", sectionService.getAllAvailableSkills()));
    }

    @PostMapping("/{id}/skills/add")
    public String addSkill(@PathVariable Long id, @Valid @ModelAttribute("skillRequest") SkillRequest req,
                           BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("cvSkills", sectionService.getSkills(id, userId));
            model.addAttribute("allSkills", sectionService.getAllAvailableSkills());
            return "candidate/cv/skills";
        }
        try {
            sectionService.addSkill(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kỹ năng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/skills";
    }

    @PostMapping("/{id}/skills/{skillId}/delete")
    public String deleteSkill(@PathVariable Long id, @PathVariable Long skillId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> sectionService.deleteSkill(id, skillId, getUserId(auth)),
                "Đã xóa kỹ năng!", "/skills");
    }

    // ---------- DỰ ÁN ----------

    @GetMapping("/{id}/projects")
    public String listProjects(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        return showSection(id, auth, model, ra,
                userId -> cvService.getCvDetail(id, userId),
                "projects", userId -> sectionService.getProjects(id, userId),
                "projectRequest", new ProjectRequest(), "candidate/cv/project", null);
    }

    @PostMapping("/{id}/projects/add")
    public String addProject(@PathVariable Long id, @Valid @ModelAttribute("projectRequest") ProjectRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvService.getCvDetail(id, userId));
            model.addAttribute("projects", sectionService.getProjects(id, userId));
            return "candidate/cv/project";
        }
        try {
            sectionService.addProject(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm dự án thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/projects";
    }

    @PostMapping("/{id}/projects/{projectId}/delete")
    public String deleteProject(@PathVariable Long id, @PathVariable Long projectId, Authentication auth, RedirectAttributes ra) {
        return deleteSectionItem(id, ra,
                () -> sectionService.deleteProject(id, projectId, getUserId(auth)),
                "Đã xóa dự án!", "/projects");
    }
}
