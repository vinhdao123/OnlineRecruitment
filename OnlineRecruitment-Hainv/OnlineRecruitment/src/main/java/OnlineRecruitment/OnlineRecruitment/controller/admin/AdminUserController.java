
package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;
import OnlineRecruitment.OnlineRecruitment.security.AppUserDetails;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import OnlineRecruitment.OnlineRecruitment.service.serviceJob.AdminUserService;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final CompanyService companyService;
    private final AdminUserService adminUserService;

    // Danh sách người dùng, tìm kiếm, lọc và phân trang
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<User> userPage =
                userService.search(keyword, role, status, page, size);

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("pageSize", size);

        var recruiterIds = userPage.getContent().stream()
                .filter(u -> u.getRole() == UserRole.RECRUITER)
                .map(User::getId)
                .toList();

        var companyMap = new HashMap<Long, String>();

        for (Long recruiterId : recruiterIds) {
            companyService.findByRecruiterId(recruiterId)
                    .ifPresent(company ->
                            companyMap.put(
                                    recruiterId,
                                    company.getCompanyName()
                            ));
        }

        model.addAttribute("companyMap", companyMap);
        model.addAttribute("activeMenu", "users");

        return "admin/users/list";
    }

    // Tạo tài khoản mới
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("activeMenu", "users");

        return "admin/users/form";
    }

    @PostMapping("/new")
    public String createNew(
            @RequestParam String username,
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
            User actor = userService.getByEmail(principal.getUsername());

            User created = userService.create(
                    username,
                    email,
                    fullName,
                    phone,
                    location,
                    password,
                    role,
                    status,
                    actor
            );

            ra.addFlashAttribute(
                    "success",
                    "Đã tạo tài khoản: " + created.getUsername()
            );

            return "redirect:/admin/users/" + created.getId();

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/users/new";
        }
    }

    // Chi tiết tài khoản
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        User user = userService.getById(id);

        model.addAttribute("user", user);

        if (user.getRole() == UserRole.RECRUITER) {
            companyService.findByRecruiterId(user.getId())
                    .ifPresent(company ->
                            model.addAttribute("company", company));
        }

        model.addAttribute("activeMenu", "users");

        return "admin/users/detail";
    }

    // Khóa tài khoản
    @PostMapping("/{id}/lock")
    public String lock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        User actor = userService.getByEmail(principal.getUsername());

        userService.lock(id, actor);

        ra.addFlashAttribute("success", "Đã khóa tài khoản.");

        return "redirect:/admin/users/" + id;
    }

    // Mở khóa tài khoản
    @PostMapping("/{id}/unlock")
    public String unlock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        User actor = userService.getByEmail(principal.getUsername());

        userService.unlock(id, actor);

        ra.addFlashAttribute("success", "Đã mở khóa tài khoản.");

        return "redirect:/admin/users/" + id;
    }

    // Xóa tài khoản — chuyển từ AdminUserController2 sang
    @PostMapping("/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser) {

        // Không cho Admin tự xóa chính mình
        if (currentUser != null
                && currentUser.getId() != null
                && currentUser.getId().equals(id)) {

            return "redirect:/admin/users?error=self";
        }

        try {
            adminUserService.deleteUser(id);

            return "redirect:/admin/users?success=deleted";

        } catch (IllegalArgumentException | IllegalStateException e) {
            String error = URLEncoder.encode(
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể xóa tài khoản",
                    StandardCharsets.UTF_8
            );

            return "redirect:/admin/users?error=" + error;
        }
    }

    // Xuất danh sách người dùng CSV
    @GetMapping("/export")
    public void exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"users_"
                        + System.currentTimeMillis()
                        + ".csv\""
        );

        PrintWriter writer = response.getWriter();
        writer.write('\ufeff');

        List<User> users =
                userService.searchAll(keyword, role, status);

        writer.println(
                "ID,Username,Full Name,Email,Phone,Role,Status,Created At"
        );

        for (User user : users) {
            writer.printf("%d,%s,%s,%s,%s,%s,%s,%s%n",
                    user.getId(),
                    csv(user.getUsername()),
                    csv(user.getFullName()),
                    csv(user.getEmail()),
                    csv(user.getPhone()),
                    user.getRole(),
                    user.getStatus(),
                    user.getCreatedAt()
            );
        }

        writer.flush();
    }

    // Form chỉnh sửa người dùng
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User user = userService.getById(id);

        model.addAttribute("user", user);
        model.addAttribute("roles", UserRole.values());
        model.addAttribute("statuses", UserStatus.values());
        model.addAttribute("activeMenu", "users");

        return "admin/users/edit-form";
    }

    // Cập nhật người dùng
    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String location,
            @RequestParam UserRole role,
            @RequestParam UserStatus status,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes ra) {

        try {
            User actor = userService.getByEmail(principal.getUsername());

            userService.update(
                    id,
                    fullName,
                    email,
                    phone,
                    location,
                    role,
                    status,
                    actor
            );

            ra.addFlashAttribute("success", "Đã cập nhật tài khoản.");

            return "redirect:/admin/users/" + id;

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());

            return "redirect:/admin/users/" + id + "/edit";
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",")
                || value.contains("\"")
                || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}