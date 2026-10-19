package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.*;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.CvService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidate/cvs")
@RequiredArgsConstructor
public class CvController {

    private final CvService cvService;
    private final UserRepository userRepository;

    // Helper lấy userId
    private Long getUserId(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
        return user.getId();
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        try {
            var cvs = cvService.listMyCvs(getUserId(auth));
            model.addAttribute("cvs", cvs);
            model.addAttribute("hasCvs", !cvs.isEmpty());
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("hasCvs", false);
        }
        return "candidate/cv/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Authentication auth, Model model) {
        model.addAttribute("cvRequest", new CreateCvRequest());
        model.addAttribute("templates", cvService.getAvailableTemplates());
        model.addAttribute("isEdit", false);
        return "candidate/cv/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("cvRequest") CreateCvRequest req,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("templates", cvService.getAvailableTemplates());
            model.addAttribute("isEdit", false);
            return "candidate/cv/form";
        }
        try {
            cvService.createCv(req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Tạo CV thành công!");
            return "redirect:/candidate/cvs";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("templates", cvService.getAvailableTemplates());
            model.addAttribute("isEdit", false);
            return "candidate/cv/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Cv cv = cvService.getCvDetail(id, getUserId(auth));
            model.addAttribute("cv", cv);
            return "candidate/cv/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Cv cv = cvService.getCvDetail(id, getUserId(auth));
            UpdateCvRequest req = new UpdateCvRequest(cv.getTitle(), cv.getProfessionalTitle(), cv.getSummary(), cv.getAccentColor());
            model.addAttribute("cvRequest", req);
            model.addAttribute("cvId", id);
            model.addAttribute("isEdit", true);
            return "candidate/cv/form";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("cvRequest") UpdateCvRequest req,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cvId", id);
            model.addAttribute("isEdit", true);
            return "candidate/cv/form";
        }
        try {
            cvService.updateCv(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Cập nhật CV thành công!");
            return "redirect:/candidate/cvs/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("cvId", id);
            model.addAttribute("isEdit", true);
            return "candidate/cv/form";
        }
    }

    @PostMapping("/{id}/default")
    public String setDefault(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.setDefault(id, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã đặt làm CV mặc định!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.softDelete(id, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa CV!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }
    @org.springframework.beans.factory.annotation.Autowired
    private OnlineRecruitment.OnlineRecruitment.repository.candidate.CvWorkExperienceRepository workRepo;
    // (Hoặc inject qua constructor nếu bạn muốn, nhưng dùng @Autowired cho nhanh ở bước này)

    @GetMapping("/{id}/work-experiences")
    public String listWorkExp(@PathVariable Long id, Authentication auth, Model model) {
        try {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("works", cvService.getWorkExperiences(id, getUserId(auth)));
            model.addAttribute("workRequest", new OnlineRecruitment.OnlineRecruitment.dto.candidate.WorkExperienceRequest());
            return "candidate/cv/work-experience";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/work-experiences/add")
    public String addWorkExp(@PathVariable Long id,
                             @jakarta.validation.Valid @ModelAttribute("workRequest") OnlineRecruitment.OnlineRecruitment.dto.candidate.WorkExperienceRequest req,
                             org.springframework.validation.BindingResult result,
                             Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("works", cvService.getWorkExperiences(id, getUserId(auth)));
            return "candidate/cv/work-experience";
        }
        try {
            cvService.addWorkExperience(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kinh nghiệm thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/work-experiences";
    }

    @PostMapping("/{id}/work-experiences/{workId}/delete")
    public String deleteWorkExp(@PathVariable Long id, @PathVariable Long workId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteWorkExperience(id, workId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa kinh nghiệm!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/work-experiences";
    }
    @GetMapping("/{id}/educations")
    public String listEdu(@PathVariable Long id, Authentication auth, Model model) {
        try {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("educations", cvService.getEducations(id, getUserId(auth)));
            model.addAttribute("eduRequest", new OnlineRecruitment.OnlineRecruitment.dto.candidate.EducationRequest());
            return "candidate/cv/education";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/educations/add")
    public String addEdu(@PathVariable Long id,
                         @jakarta.validation.Valid @ModelAttribute("eduRequest") OnlineRecruitment.OnlineRecruitment.dto.candidate.EducationRequest req,
                         org.springframework.validation.BindingResult result,
                         Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("educations", cvService.getEducations(id, getUserId(auth)));
            return "candidate/cv/education";
        }
        try {
            cvService.addEducation(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm học vấn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/educations";
    }

    @PostMapping("/{id}/educations/{eduId}/delete")
    public String deleteEdu(@PathVariable Long id, @PathVariable Long eduId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteEducation(id, eduId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa học vấn!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/educations";
    }
    @GetMapping("/{id}/skills")
    public String listSkills(@PathVariable Long id, Authentication auth, Model model) {
        try {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("cvSkills", cvService.getSkills(id, getUserId(auth)));
            model.addAttribute("allSkills", cvService.getAllAvailableSkills());
            model.addAttribute("skillRequest", new OnlineRecruitment.OnlineRecruitment.dto.candidate.SkillRequest());
            return "candidate/cv/skills";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/skills/add")
    public String addSkill(@PathVariable Long id,
                           @jakarta.validation.Valid @ModelAttribute("skillRequest") OnlineRecruitment.OnlineRecruitment.dto.candidate.SkillRequest req,
                           org.springframework.validation.BindingResult result,
                           Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("cvSkills", cvService.getSkills(id, getUserId(auth)));
            model.addAttribute("allSkills", cvService.getAllAvailableSkills());
            return "candidate/cv/skills";
        }
        try {
            cvService.addSkill(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm kỹ năng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/skills";
    }

    @PostMapping("/{id}/skills/{skillId}/delete")
    public String deleteSkill(@PathVariable Long id, @PathVariable Long skillId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteSkill(id, skillId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa kỹ năng!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/skills";
    }
    @GetMapping("/{id}/projects")
    public String listProjects(@PathVariable Long id, Authentication auth, Model model) {
        try {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("projects", cvService.getProjects(id, getUserId(auth)));
            model.addAttribute("projectRequest", new OnlineRecruitment.OnlineRecruitment.dto.candidate.ProjectRequest());
            return "candidate/cv/project";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/projects/add")
    public String addProject(@PathVariable Long id,
                             @jakarta.validation.Valid @ModelAttribute("projectRequest") OnlineRecruitment.OnlineRecruitment.dto.candidate.ProjectRequest req,
                             org.springframework.validation.BindingResult result,
                             Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("projects", cvService.getProjects(id, getUserId(auth)));
            return "candidate/cv/project";
        }
        try {
            cvService.addProject(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Thêm dự án thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/projects";
    }

    @PostMapping("/{id}/projects/{projectId}/delete")
    public String deleteProject(@PathVariable Long id, @PathVariable Long projectId, Authentication auth, RedirectAttributes ra) {
        try {
            cvService.deleteProject(id, projectId, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Đã xóa dự án!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + id + "/projects";
    }

    @GetMapping("/{id}/preview")
    public String previewCv(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        try {
            java.util.Map<String, Object> cvData = cvService.getFullCvForPreview(id, getUserId(auth));
            model.addAllAttributes(cvData);
            return "candidate/cv/preview";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }
}