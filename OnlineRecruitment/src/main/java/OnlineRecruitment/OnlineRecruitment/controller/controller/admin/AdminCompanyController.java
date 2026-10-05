package OnlineRecruitment.OnlineRecruitment.controller.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/companies")
@RequiredArgsConstructor
public class AdminCompanyController {

    private final CompanyService companyService;
    private final UserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) CompanyStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        Page<Company> companyPage = companyService.search(keyword, status, page, size);

        model.addAttribute("companies",      companyPage.getContent());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     companyPage.getTotalPages());
        model.addAttribute("totalItems",     companyPage.getTotalElements());
        model.addAttribute("keyword",        keyword);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses",       CompanyStatus.values());
        return "admin/companies/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Company company = companyService.getById(id);
        model.addAttribute("company", company);
        return "admin/companies/detail";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails principal,
                          RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        companyService.approve(id, actor);
        ra.addFlashAttribute("success", "Đã duyệt công ty.");
        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        companyService.reject(id, reason, actor);
        ra.addFlashAttribute("success", "Đã từ chối công ty.");
        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails principal,
                       RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        companyService.lock(id, actor);
        ra.addFlashAttribute("success", "Đã khóa công ty.");
        return "redirect:/admin/companies/" + id;
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        companyService.unlock(id, actor);
        ra.addFlashAttribute("success", "Đã mở khóa công ty.");
        return "redirect:/admin/companies/" + id;
    }
}