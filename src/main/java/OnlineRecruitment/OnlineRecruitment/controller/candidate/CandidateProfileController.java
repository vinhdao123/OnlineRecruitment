package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CandidateProfileDto;
import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import OnlineRecruitment.OnlineRecruitment.service.CandidateService;
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
@RequestMapping("/candidate")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final UserRepository userRepository;
    private final CandidateService candidateService;
    private final AuthService authService;

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng hiện tại"));
    }

    // 1. Xem & Cập nhật Profile
    @GetMapping("/profile")
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(value = "edit", defaultValue = "false") boolean isEdit,
                              Model model) {
        User user = getAuthenticatedUser(userDetails);
        Candidate candidate = candidateService.getOrCreateCandidate(user);

        if (!model.containsAttribute("candidateDto")) {
            CandidateProfileDto dto = new CandidateProfileDto();
            dto.setFullName(user.getFullName());
            dto.setPhone(user.getPhone());
            dto.setAddress(candidate.getAddress() != null ? candidate.getAddress() : user.getLocation());
            dto.setDob(candidate.getDob());
            dto.setGender(candidate.getGender());
            dto.setDesiredPosition(candidate.getDesiredPosition());
            dto.setAboutMe(candidate.getAboutMe());
            dto.setLinkedinUrl(candidate.getLinkedinUrl());
            dto.setGithubUrl(candidate.getGithubUrl());
            dto.setPortfolioUrl(candidate.getPortfolioUrl());
            model.addAttribute("candidateDto", dto);
        }

        model.addAttribute("profileUser", user);
        model.addAttribute("candidate", candidate);
        model.addAttribute("isEditMode", isEdit);

        return "candidate/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("candidateDto") CandidateProfileDto dto,
                                BindingResult result,
                                Model model,
                                RedirectAttributes ra) {
        User user = getAuthenticatedUser(userDetails);

        if (result.hasErrors()) {
            Candidate candidate = candidateService.getOrCreateCandidate(user);
            model.addAttribute("profileUser", user);
            model.addAttribute("candidate", candidate);
            model.addAttribute("isEditMode", true);
            return "candidate/profile";
        }

        try {
            candidateService.updateProfile(user, dto);
            ra.addFlashAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công!");
            return "redirect:/candidate/profile";
        } catch (Exception e) {
            Candidate candidate = candidateService.getOrCreateCandidate(user);
            model.addAttribute("profileUser", user);
            model.addAttribute("candidate", candidate);
            model.addAttribute("isEditMode", true);
            model.addAttribute("errorMessage", "Cập nhật thất bại: " + e.getMessage());
            return "candidate/profile";
        }
    }

    // 2. Đổi mật khẩu qua Gmail OTP
    @GetMapping("/change-password")
    public String showChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = getAuthenticatedUser(userDetails);
        model.addAttribute("accountUser", user);
        return "candidate/change-password";
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
        return "redirect:/candidate/change-password";
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
            return "redirect:/candidate/change-password";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("inputEmail", email);
            ra.addFlashAttribute("inputOtp", otp);
            return "redirect:/candidate/change-password";
        }
    }
}
