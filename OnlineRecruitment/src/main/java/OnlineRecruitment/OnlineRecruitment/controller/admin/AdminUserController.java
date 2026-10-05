package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final CompanyService companyService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) UserRole role,
                       @RequestParam(required = false) UserStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {

        Page<User> userPage = userService.search(keyword, role, status, page, size);

        model.addAttribute("users",          userPage.getContent());
        model.addAttribute("currentPage",    page);
        model.addAttribute("totalPages",     userPage.getTotalPages());
        model.addAttribute("totalItems",     userPage.getTotalElements());
        model.addAttribute("keyword",        keyword);
        model.addAttribute("selectedRole",   role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles",          UserRole.values());
        model.addAttribute("statuses",       UserStatus.values());
        model.addAttribute("pageSize",       size);

        var recruiterIds = userPage.getContent().stream()
                .filter(u -> u.getRole() == UserRole.RECRUITER)
                .map(User::getId)
                .toList();
        var companyMap = new java.util.HashMap<Long, String>();
        for (Long rid : recruiterIds) {
            companyService.findByRecruiterId(rid)
                    .ifPresent(c -> companyMap.put(rid, c.getCompanyName()));
        }
        model.addAttribute("companyMap", companyMap);

        model.addAttribute("activeMenu", "users");
        return "admin/users/list";
    }

    /** Form tạo user mới. */
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("roles",    UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("activeMenu", "users");
        return "admin/users/form";
    }

    /** Xử lý submit form tạo user. */
    @PostMapping("/new")
    public String createNew(@RequestParam String username,
                            @RequestParam String email,
                            @RequestParam String fullName,
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String location,
                            @RequestParam String password,
                            @RequestParam UserRole role,
                            @RequestParam UserStatus status,
                            @AuthenticationPrincipal UserDetails principal,
                            RedirectAttributes ra) {
        try {
            User actor = userService.getByUsername(principal.getUsername());
            User created = userService.create(username, email, fullName,
                    phone, location, password, role, status, actor);
            ra.addFlashAttribute("success", "Đã tạo tài khoản: " + created.getUsername());
            return "redirect:/admin/users/" + created.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        model.addAttribute("user", user);

        if (user.getRole() == UserRole.RECRUITER) {
            companyService.findByRecruiterId(user.getId())
                    .ifPresent(c -> model.addAttribute("company", c));
        }
        model.addAttribute("activeMenu", "users");
        return "admin/users/detail";
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails principal,
                       RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        userService.lock(id, actor);
        ra.addFlashAttribute("success", "Đã khóa tài khoản.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes ra) {
        User actor = userService.getByUsername(principal.getUsername());
        userService.unlock(id, actor);
        ra.addFlashAttribute("success", "Đã mở khóa tài khoản.");
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/export")
    public void exportCsv(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) UserRole role,
                          @RequestParam(required = false) UserStatus status,
                          HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"users_" + System.currentTimeMillis() + ".csv\"");

        response.getWriter().write('\ufeff');

        List<User> users = userService.searchAll(keyword, role, status);

        PrintWriter w = response.getWriter();
        w.println("ID,Username,Full Name,Email,Phone,Role,Status,Created At");
        for (User u : users) {
            w.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                    u.getId(),
                    csv(u.getUsername()),
                    csv(u.getFullName()),
                    csv(u.getEmail()),
                    csv(u.getPhone()),
                    u.getRole(),
                    u.getStatus(),
                    u.getCreatedAt()
            );
        }
        w.flush();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}