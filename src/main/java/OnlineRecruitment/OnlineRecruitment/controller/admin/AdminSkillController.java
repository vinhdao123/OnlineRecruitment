package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import OnlineRecruitment.OnlineRecruitment.service.AdminSkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Admin Skill Flow: xem danh sách -> tạo mới hoặc sửa -> Service -> Repository -> skills. */
@Controller
@RequestMapping("/admin/skills")
public class AdminSkillController {
    private final AdminSkillService service;
    public AdminSkillController(AdminSkillService service) { this.service = service; }

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "skills"; }

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Long categoryId, Model model) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);
        model.addAttribute("skills", service.getAll().stream()
                .filter(skill -> query.isEmpty() || skill.getName().toLowerCase(java.util.Locale.ROOT).contains(query))
                .filter(skill -> status == null || status.isBlank() || status.equals(skill.getStatus()))
                .filter(skill -> categoryId == null || (skill.getCategory() != null && categoryId.equals(skill.getCategory().getId())))
                .toList());
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", service.getCategories());
        return "admin/skills/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("skillForm", new SkillForm());
        model.addAttribute("categories", service.getCategories());
        model.addAttribute("pageTitle", "Tạo kỹ năng");
        return "admin/skills/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("skillForm") SkillForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return invalid(model, "Tạo kỹ năng");
        try {
            service.create(form.getName(), form.getCategoryId());
            redirect.addFlashAttribute("successMessage", "Tạo kỹ năng thành công.");
            return "redirect:/admin/skills";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return invalid(model, "Tạo kỹ năng");
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        try {
            Skill skill = service.getById(id);
            SkillForm form = new SkillForm();
            form.setName(skill.getName());
            form.setCategoryId(skill.getCategory() == null ? null : skill.getCategory().getId());
            form.setStatus(skill.getStatus());
            model.addAttribute("skillForm", form);
            model.addAttribute("skillId", id);
            return invalid(model, "Sửa kỹ năng");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/skills";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("skillForm") SkillForm form,
                         BindingResult errors, Model model, RedirectAttributes redirect) {
        model.addAttribute("skillId", id);
        if (errors.hasErrors()) return invalid(model, "Sửa kỹ năng");
        try {
            service.update(id, form.getName(), form.getCategoryId(), form.getStatus());
            redirect.addFlashAttribute("successMessage", "Cập nhật kỹ năng thành công.");
            return "redirect:/admin/skills";
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return invalid(model, "Sửa kỹ năng");
        }
    }

    // Dùng lại cùng template form để tránh nhân đôi HTML.
    private String invalid(Model model, String title) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("categories", service.getCategories());
        return "admin/skills/form";
    }

    public static class SkillForm {
        @NotBlank(message = "Hãy nhập tên kỹ năng.")
        @Size(max = 150, message = "Tên tối đa 150 ký tự.")
        private String name;
        @NotNull(message = "Hãy chọn danh mục ngành nghề.")
        private Long categoryId;
        private String status = "ACTIVE";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
