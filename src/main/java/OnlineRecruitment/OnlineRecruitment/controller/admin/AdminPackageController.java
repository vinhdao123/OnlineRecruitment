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
    // CHẶN CREATE: redirect về list kèm thông báo
    // ============================================
    @GetMapping("/new")
    public String createForm(RedirectAttributes ra) {
        ra.addFlashAttribute("error",
                "Chức năng tạo gói mới đã bị vô hiệu hóa. Admin chỉ được sửa giá, thời hạn và trạng thái.");
        return "redirect:/admin/packages";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pkg", packageService.getById(id));

        // Chỉ lấy RECRUITER và CANDIDATE
        List<UserRole> allowedRoles = Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN)
                .toList();
        model.addAttribute("roles", allowedRoles);

        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form";
    }

    // ============================================
    // CHỈ CHO UPDATE 3 FIELD: price, durationDays, isActive
    // ============================================
    @PostMapping("/save")
    public String save(@ModelAttribute("pkg") ServicePackage form,
                       RedirectAttributes ra) {

        // 1. CHẶN CREATE — chỉ cho Update
        if (form.getId() == null) {
            ra.addFlashAttribute("error", "Chức năng tạo gói mới đã bị vô hiệu hóa.");
            return "redirect:/admin/packages";
        }

        // 2. Tìm entity gốc trong DB
        ServicePackage existing;
        try {
            existing = packageService.getById(form.getId());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Không tìm thấy gói cần sửa.");
            return "redirect:/admin/packages";
        }

        // 3. Validate 3 field được phép sửa
        if (form.getPrice() == null || form.getPrice() <= 0) {
            ra.addFlashAttribute("error", "Giá gói phải lớn hơn 0.");
            return "redirect:/admin/packages/" + form.getId() + "/edit";   // ✅ SỬA
        }
        if (form.getPrice() % 1 != 0) {
            ra.addFlashAttribute("error", "Giá gói phải là số nguyên (VNĐ).");
            return "redirect:/admin/packages/" + form.getId() + "/edit";   // ✅ SỬA
        }
        if (form.getDurationDays() == null || form.getDurationDays() < 1) {
            ra.addFlashAttribute("error", "Thời hạn phải ít nhất 1 ngày.");
            return "redirect:/admin/packages/" + form.getId() + "/edit";   // ✅ SỬA
        }

        // 4. CHỈ COPY 3 FIELD ĐƯỢC PHÉP
        existing.setPrice(form.getPrice());
        existing.setDurationDays(form.getDurationDays());
        existing.setIsActive(form.getIsActive() != null && form.getIsActive());

        // 5. Lưu — name/description/targetRole giữ nguyên từ DB
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
}