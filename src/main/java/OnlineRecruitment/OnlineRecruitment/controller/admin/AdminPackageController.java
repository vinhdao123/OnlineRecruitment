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

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pkg", new ServicePackage());

        // SỬA DÒNG NÀY: Chỉ lấy RECRUITER và CANDIDATE
        List<UserRole> allowedRoles = Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN)
                .toList();
        model.addAttribute("roles", allowedRoles);

        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pkg", packageService.getById(id));

        // SỬA DÒNG NÀY: Chỉ lấy RECRUITER và CANDIDATE
        List<UserRole> allowedRoles = Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN)
                .toList();
        model.addAttribute("roles", allowedRoles);

        model.addAttribute("activeMenu", "packages");
        return "admin/packages/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("pkg") ServicePackage pkg, RedirectAttributes ra) {
        packageService.save(pkg);
        ra.addFlashAttribute("success", "Đã lưu gói dịch vụ thành công.");
        return "redirect:/admin/packages";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        packageService.toggleStatus(id);
        ra.addFlashAttribute("success", "Đã thay đổi trạng thái gói dịch vụ.");
        return "redirect:/admin/packages";
    }
}