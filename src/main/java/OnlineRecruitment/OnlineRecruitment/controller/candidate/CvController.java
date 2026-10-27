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
import org.springframework.web.multipart.MultipartFile;
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
            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

            var cvs = cvService.listMyCvs(user.getId());
            var cvDataMap = cvService.getCvListWithDetails(user.getId());

            model.addAttribute("cvs", cvs);
            model.addAttribute("hasCvs", !cvs.isEmpty());
            model.addAttribute("profileUser", user);
            model.addAttribute("cvDataMap", cvDataMap);
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("hasCvs", false);
        }
        return "candidate/cv/list";
    }

    @GetMapping("/new")
    public String showCreateForm(
            @RequestParam(value = "templateId", required = false) Long templateId,
            @RequestParam(value = "sourceCvId", required = false) Long sourceCvId,
            Authentication auth, Model model) {

        CreateCvRequest req = new CreateCvRequest();

        if (templateId != null) {
            req.setTemplateVersionId(templateId);
        }

        model.addAttribute("cvRequest", req);
        model.addAttribute("templates", cvService.getAvailableTemplates());
        model.addAttribute("isEdit", false);

        if (sourceCvId != null) {
            model.addAttribute("sourceCvId", sourceCvId);
        }

        return "candidate/cv/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("cvRequest") CreateCvRequest req,
                         BindingResult result,
                         @RequestParam(value = "sourceCvId", required = false) Long sourceCvId,
                         Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("templates", cvService.getAvailableTemplates());
            model.addAttribute("isEdit", false);
            return "candidate/cv/form";
        }
        try {
            Long newCvId = cvService.createCvAndReturnId(req, getUserId(auth));

            if (sourceCvId != null) {
                cvService.copyCvData(sourceCvId, newCvId, getUserId(auth));
            }

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
    public String detail(@PathVariable Long id,
                         @RequestParam(value = "edit", required = false, defaultValue = "false") boolean isEdit,
                         Authentication auth, Model model, RedirectAttributes ra) {
        try {
            Cv cv = cvService.getCvDetail(id, getUserId(auth));
            model.addAttribute("cv", cv);
            model.addAttribute("isEditMode", isEdit);

            if (isEdit) {
                UpdateCvRequest req = new UpdateCvRequest(cv.getTitle(), cv.getProfessionalTitle(), cv.getSummary(), cv.getAccentColor(), cv.getAvatarUrl());
                model.addAttribute("cvRequest", req);
            }
            return "candidate/cv/detail";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/update")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("cvRequest") UpdateCvRequest req,
                         BindingResult result,
                         @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                         Authentication auth, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("cvId", id);
            model.addAttribute("isEditMode", true);
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            return "candidate/cv/detail";
        }
        try {
            if (avatarFile != null && !avatarFile.isEmpty()) {
                try {
                    String uploadDir = System.getProperty("user.dir") + "/uploads/";
                    java.io.File dir = new java.io.File(uploadDir);
                    if (!dir.exists()) dir.mkdirs();

                    String fileName = System.currentTimeMillis() + "_" + avatarFile.getOriginalFilename();
                    java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + fileName);
                    java.nio.file.Files.copy(avatarFile.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    req.setAvatarUrl("/uploads/" + fileName);
                } catch (java.io.IOException e) {
                    throw new RuntimeException("Lỗi khi lưu ảnh: " + e.getMessage());
                }
            }

            cvService.updateCv(id, req, getUserId(auth));
            ra.addFlashAttribute("successMessage", "Cập nhật thông tin CV thành công!");
            return "redirect:/candidate/cvs/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("cvId", id);
            model.addAttribute("isEditMode", true);
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            return "candidate/cv/detail";
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

    // ==========================================
    // CÁC ROUTE CHO PHẦN NỘI DUNG CV
    // ==========================================

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

    @GetMapping("/{id}/work-experiences")
    public String listWorkExp(@PathVariable Long id, Authentication auth, Model model) {
        try {
            model.addAttribute("cv", cvService.getCvDetail(id, getUserId(auth)));
            model.addAttribute("works", cvService.getWorkExperiences(id, getUserId(auth)));
            model.addAttribute("workRequest", new WorkExperienceRequest());
            return "candidate/cv/work-experience";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/work-experiences/add")
    public String addWorkExp(@PathVariable Long id, @Valid @ModelAttribute("workRequest") WorkExperienceRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
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
            model.addAttribute("eduRequest", new EducationRequest());
            return "candidate/cv/education";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/educations/add")
    public String addEdu(@PathVariable Long id, @Valid @ModelAttribute("eduRequest") EducationRequest req,
                         BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
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
            model.addAttribute("skillRequest", new SkillRequest());
            return "candidate/cv/skills";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/skills/add")
    public String addSkill(@PathVariable Long id, @Valid @ModelAttribute("skillRequest") SkillRequest req,
                           BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
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
            model.addAttribute("projectRequest", new ProjectRequest());
            return "candidate/cv/project";
        } catch (Exception e) {
            return "redirect:/candidate/cvs";
        }
    }

    @PostMapping("/{id}/projects/add")
    public String addProject(@PathVariable Long id, @Valid @ModelAttribute("projectRequest") ProjectRequest req,
                             BindingResult result, Authentication auth, Model model, RedirectAttributes ra) {
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

    @GetMapping("/templates")
    public String listTemplates() {
        return "candidate/cv/templates";
    }

    @PostMapping("/upload")
    public String uploadCv(@RequestParam("cvFile") MultipartFile file, Authentication auth, RedirectAttributes ra) {
        try {
            if (file.isEmpty()) {
                ra.addFlashAttribute("errorMessage", "Vui lòng chọn file!");
                return "redirect:/candidate/cvs";
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !(originalName.endsWith(".pdf") || originalName.endsWith(".doc") || originalName.endsWith(".docx"))) {
                ra.addFlashAttribute("errorMessage", "Chỉ chấp nhận file PDF hoặc Word!");
                return "redirect:/candidate/cvs";
            }

            String uploadDir = System.getProperty("user.dir") + "/uploads/cv/";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            String fileName = System.currentTimeMillis() + "_" + originalName;
            java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + fileName);
            java.nio.file.Files.copy(file.getInputStream(), path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Gọi hàm với 2 tham số
            cvService.uploadCvFile("/uploads/cv/" + fileName, getUserId(auth));

            ra.addFlashAttribute("successMessage", "Tải CV lên thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi tải file: " + e.getMessage());
        }
        return "redirect:/candidate/cvs";
    }
}