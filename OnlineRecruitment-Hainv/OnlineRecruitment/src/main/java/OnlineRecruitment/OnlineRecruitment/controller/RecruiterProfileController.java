package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.dto.CompanyProfileUpdateDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterProfileDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterProfileUpdateDto;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.security.AppUserDetails;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recruiter/profile")
public class RecruiterProfileController {

    private final UserService userService;
    private final CompanyService companyService;

    // 1. Xem hồ sơ cá nhân
    @GetMapping
    public String viewProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            Model model
    ) {
        requireRecruiter(principal);

        RecruiterProfileDto profile =
                userService.getRecruiterProfile(principal.getId());

        model.addAttribute("profile", profile);

        return "recruiter/profile";
    }

    // 2. Hiển thị form cập nhật hồ sơ cá nhân
    @GetMapping("/edit")
    public String editProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            Model model
    ) {
        requireRecruiter(principal);

        RecruiterProfileDto profile =
                userService.getRecruiterProfile(principal.getId());

        model.addAttribute("profile", profile);
        model.addAttribute(
                "form",
                new RecruiterProfileUpdateDto(
                        profile.fullName(),
                        profile.email(),
                        profile.phone(),
                        profile.location(),
                        profile.avatarUrl()
                )
        );

        return "recruiter/profile-edit";
    }

    // 3. Lưu hồ sơ cá nhân
    @PostMapping("/edit")
    public String updateProfile(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @ModelAttribute("form")
            RecruiterProfileUpdateDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireRecruiter(principal);

        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "profile",
                    userService.getRecruiterProfile(principal.getId())
            );
            return "recruiter/profile-edit";
        }

        try {
            userService.updateRecruiterProfile(principal.getId(), form);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật hồ sơ cá nhân thành công!"
            );
            return "redirect:/recruiter/profile";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute(
                    "profile",
                    userService.getRecruiterProfile(principal.getId())
            );
            return "recruiter/profile-edit";
        }
    }

    // 4. Hiển thị hồ sơ công ty
    @GetMapping("/company")
    public String viewCompany(
            @AuthenticationPrincipal AppUserDetails principal,
            Model model
    ) {
        requireRecruiter(principal);

        Company company = companyService
                .findByRecruiterId(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có hồ sơ công ty"
                ));

        model.addAttribute("company", company);

        return "recruiter/company-profile";
    }

    // 5. Hiển thị form cập nhật công ty
    @GetMapping("/company/edit")
    public String editCompany(
            @AuthenticationPrincipal AppUserDetails principal,
            Model model
    ) {
        requireRecruiter(principal);

        Company company = companyService
                .findByRecruiterId(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa có hồ sơ công ty"
                ));

        model.addAttribute("company", company);
        model.addAttribute(
                "form",
                new CompanyProfileUpdateDto(
                        company.getCompanyName(),
                        company.getTaxCode(),
                        company.getEmail(),
                        company.getPhone(),
                        company.getAddress(),
                        company.getCompanySize(),
                        company.getWebsite(),
                        company.getDescription(),
                        company.getLogoUrl()
                )
        );

        return "recruiter/company-edit";
    }

    // 6. Lưu hồ sơ công ty
    @PostMapping("/company/edit")
    public String updateCompany(
            @AuthenticationPrincipal AppUserDetails principal,
            @Valid @ModelAttribute("form")
            CompanyProfileUpdateDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        requireRecruiter(principal);

        if (bindingResult.hasErrors()) {
            Company company = companyService
                    .findByRecruiterId(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy hồ sơ công ty"
                    ));

            model.addAttribute("company", company);
            return "recruiter/company-edit";
        }

        try {
            companyService.updateRecruiterCompany(
                    principal.getId(),
                    form
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Cập nhật hồ sơ công ty thành công!"
            );

            return "redirect:/recruiter/profile/company";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());

            Company company = companyService
                    .findByRecruiterId(principal.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy hồ sơ công ty"
                    ));

            model.addAttribute("company", company);
            return "recruiter/company-edit";
        }
    }

    private void requireRecruiter(AppUserDetails principal) {
        if (principal == null
                || !"RECRUITER".equals(principal.getRole())) {
            throw new AccessDeniedException(
                    "Bạn cần đăng nhập bằng tài khoản recruiter"
            );
        }
    }
}