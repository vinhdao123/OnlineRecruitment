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

    // 1. Giữ nguyên hàm hiển thị danh sách
    @GetMapping
    public String list(Model model) {
        model.addAttribute("packages", packageService.getAllPackages());
        model.addAttribute("activeMenu", "packages");
        return "admin/packages/list";
    }

    // 2. Giữ nguyên hàm Chỉnh sửa (Sửa giá, sửa mô tả)
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pkg", packageService.getById(id));
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form"; // Vẫn dùng form cũ để sửa
    }

    // 3. Giữ nguyên hàm Lưu (chỉ dùng để cập nhật gói cũ)
    @PostMapping("/save")
    public String save(@ModelAttribute("pkg") ServicePackage pkg, RedirectAttributes ra) {
        packageService.save(pkg);
        ra.addFlashAttribute("success", "Đã cập nhật gói dịch vụ thành công.");
        return "redirect:/admin/packages";
    }

    // 4. Giữ nguyên hàm Ẩn/Hiện (Bật/tắt trạng thái bán)
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        packageService.toggleStatus(id);
        ra.addFlashAttribute("success", "Đã thay đổi trạng thái gói dịch vụ.");
        return "redirect:/admin/packages";
    }

}