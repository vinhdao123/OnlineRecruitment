package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.ServicePackage;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.service.ServicePackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/packages")
@RequiredArgsConstructor
public class AdminPackageController {

    private final ServicePackageService packageService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("packages", packageService.getAllPackages());
        model.addAttribute("activeMenu", "packages");
        return "admin/packages/list";
    }

    // ============================================
    // MỞ KHÓA TẠO GÓI MỚI
    // ============================================
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pkg", new ServicePackage());
        model.addAttribute("roles", allowedRoles());
        model.addAttribute("isNew", true); // Cờ đánh dấu để HTML biết là Tạo mới
        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pkg", packageService.getById(id));
        model.addAttribute("roles", allowedRoles());
        model.addAttribute("isNew", false); // Cờ đánh dấu để HTML biết là Chỉnh sửa
        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("pkg") ServicePackage form,
                       RedirectAttributes ra) {

        // Lấy type chuẩn xác, nếu null thì mặc định là MAIN
        String type = form.getPackageType();
        if (type == null || type.trim().isEmpty()) {
            type = "MAIN";
        }

        // 1. Validate chung
        if (form.getPrice() == null || form.getPrice() <= 0) {
            ra.addFlashAttribute("error", "Giá gói phải lớn hơn 0.");
            return redirectBack(form);
        }
        if (form.getPrice() % 1 != 0) {
            ra.addFlashAttribute("error", "Giá gói phải là số nguyên (VNĐ).");
            return redirectBack(form);
        }

        // 2. Validate theo loại gói
        if ("TOPUP".equals(type)) {
            // Nếu là TOPUP thì chỉ cần check số tin
            if (form.getMaxJobs() == null || form.getMaxJobs() < 1) {
                ra.addFlashAttribute("error", "Gói mua thêm tin phải có số tin > 0.");
                return redirectBack(form);
            }
            form.setDurationDays(null); // Topup không có hạn
            form.setAllowPinJob(false); // Topup không được ghim
        } else {
            // Nếu là MAIN thì bắt buộc check thời hạn
            if (form.getDurationDays() == null || form.getDurationDays() < 1) {
                ra.addFlashAttribute("error", "Gói chính phải có thời hạn >= 1 ngày.");
                return redirectBack(form);
            }
        }

        // 3. Xử lý TẠO MỚI
        if (form.getId() == null) {
            if (form.getName() == null || form.getName().isBlank()) {
                ra.addFlashAttribute("error", "Tên gói không được để trống.");
                return "redirect:/admin/packages/new";
            }

            form.setTargetRole(UserRole.RECRUITER); // Tự động gán Recruiter
            if (form.getIsActive() == null) form.setIsActive(true);

            packageService.save(form);
            ra.addFlashAttribute("success", "Đã tạo gói " + form.getName() + " thành công.");
            return "redirect:/admin/packages";
        }

        // 4. Xử lý CẬP NHẬT
        ServicePackage existing = packageService.getById(form.getId());
        existing.setPrice(form.getPrice());
        existing.setMaxJobs(form.getMaxJobs());
        existing.setAllowPinJob(Boolean.TRUE.equals(form.getAllowPinJob()));
        existing.setIsActive(Boolean.TRUE.equals(form.getIsActive()));

        if ("MAIN".equals(type)) {
            existing.setDurationDays(form.getDurationDays());
        }

        packageService.save(existing);
        ra.addFlashAttribute("success", "Đã cập nhật gói " + existing.getName() + " thành công.");
        return "redirect:/admin/packages";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        packageService.toggleStatus(id);
        ra.addFlashAttribute("success", "Đã thay đổi trạng thái gói dịch vụ.");
        return "redirect:/admin/packages";
    }

    private List<UserRole> allowedRoles() {
        return List.of(UserRole.RECRUITER);
    }

    private String redirectBack(ServicePackage form) {
        return form.getId() == null
                ? "redirect:/admin/packages/new"
                : "redirect:/admin/packages/" + form.getId() + "/edit";
    }
}