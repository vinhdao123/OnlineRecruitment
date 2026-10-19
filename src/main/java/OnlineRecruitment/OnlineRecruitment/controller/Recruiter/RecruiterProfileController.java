package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterProfileDto;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/recruiter")
@RequiredArgsConstructor
public class RecruiterProfileController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final AuthService authService;

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy nhà tuyển dụng hiện tại"));
    }

    // 1. Xem & Cập nhật Profile Recruiter & Company
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(value = "edit", defaultValue = "false") boolean isEdit,
                              Model model) {
        User user = getAuthenticatedUser(userDetails);
        Company company = companyRepository.findByRecruiterId(user.getId()).orElse(null);

        if (!model.containsAttribute("recruiterDto")) {
            RecruiterProfileDto dto = new RecruiterProfileDto();
            dto.setFullName(user.getFullName());
            dto.setPhone(user.getPhone());
            dto.setLocation(user.getLocation());

            if (company != null) {
                dto.setCompanyName(company.getCompanyName());
                dto.setTaxCode(company.getTaxCode());
                dto.setCompanyEmail(company.getEmail());
                dto.setCompanyPhone(company.getPhone());
                dto.setCompanyAddress(company.getAddress());
                dto.setCompanyWebsite(company.getWebsite());
                dto.setCompanySize(company.getCompanySize());
                dto.setCompanyDescription(company.getDescription());
            }
            model.addAttribute("recruiterDto", dto);
        }

        model.addAttribute("profileUser", user);
        model.addAttribute("company", company);
        model.addAttribute("isEditMode", isEdit);

        return "recruiter/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("recruiterDto") RecruiterProfileDto dto,
                                BindingResult result,
                                Model model,
                                RedirectAttributes ra) {
        User user = getAuthenticatedUser(userDetails);

        if (result.hasErrors()) {
            Company company = companyRepository.findByRecruiterId(user.getId()).orElse(null);
            model.addAttribute("profileUser", user);
            model.addAttribute("company", company);
            model.addAttribute("isEditMode", true);
            return "recruiter/profile";
        }

        try {
            companyService.updateRecruiterProfile(user, dto);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ nhà tuyển dụng và doanh nghiệp thành công!");
            return "redirect:/recruiter/profile";
        } catch (Exception e) {
            Company company = companyRepository.findByRecruiterId(user.getId()).orElse(null);
            model.addAttribute("profileUser", user);
            model.addAttribute("company", company);
            model.addAttribute("isEditMode", true);
            model.addAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "recruiter/profile";
        }
    }

    // 2. Đổi mật khẩu qua Gmail OTP
    @GetMapping("/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("accountUser", user);
        return "recruiter/change-password";
    }

    @PostMapping("/change-password/send-otp")
    public String sendOtp(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam("email") String email,
                          RedirectAttributes ra) {
        User user = getAuthenticatedUser(userDetails);
        try {
            authService.sendChangePasswordOtp(user, email);
            ra.addFlashAttribute("successMessage", "Mã xác thực OTP đã được gửi đến hộp thư Gmail: " + email);
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("inputEmail", email);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("inputEmail", email);
        }
        return "redirect:/recruiter/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam("email") String email,
                                 @RequestParam("otp") String otp,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 RedirectAttributes ra) {
        User user = getAuthenticatedUser(userDetails);
        try {
            authService.changePasswordWithOtp(user, email, otp, newPassword, confirmPassword);
            ra.addFlashAttribute("successMessage", "Thay đổi mật khẩu thành công! Hãy ghi nhớ mật khẩu mới của bạn.");
            return "redirect:/recruiter/change-password";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("inputEmail", email);
            ra.addFlashAttribute("inputOtp", otp);
            return "redirect:/recruiter/change-password";
        }
    }
}
