
package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.service.JobCategoryAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminJobCategoryController {

    private final JobCategoryAdminService categoryService;

    public AdminJobCategoryController(
            JobCategoryAdminService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        model.addAttribute("pageTitle", "Create Job Category");
        return "admin/categories/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Create Job Category");
            return "admin/categories/form";
        }

        try {
            categoryService.create(
                    form.getName(), form.getDescription());

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã tạo danh mục thành công.");

            return "redirect:/admin/categories";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("pageTitle", "Create Job Category");
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/categories/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            JobCategory category = categoryService.findById(id);

            CategoryForm form = new CategoryForm();
            form.setName(category.getName());
            form.setDescription(category.getDescription());
            form.setStatus(category.getStatus());

            model.addAttribute("categoryForm", form);
            model.addAttribute("categoryId", id);
            model.addAttribute("pageTitle", "Update Job Category");

            return "admin/categories/form";

        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());

            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Update Job Category");
            model.addAttribute("categoryId", id);
            return "admin/categories/form";
        }

        try {
            categoryService.update(
                    id, form.getName(), form.getDescription(), form.getStatus());

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Đã cập nhật danh mục thành công.");

            return "redirect:/admin/categories";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("pageTitle", "Update Job Category");
            model.addAttribute("categoryId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/categories/form";
        }
    }

    public static class CategoryForm {

        @NotBlank(message = "Tên danh mục không được để trống.")
        @Size(max = 150,
                message = "Tên danh mục tối đa 150 ký tự.")
        private String name;

        private String description;

        // Chỉ áp dụng khi cập nhật; tạo mới luôn mặc định ACTIVE.
        private String status = "ACTIVE";

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
