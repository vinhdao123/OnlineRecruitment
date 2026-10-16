package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.dto.auth.VerifyOtpDto;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/verify-otp")
    public String showVerifyOtpForm(@RequestParam(value = "email", required = false) String email, Model model) {
        if (!model.containsAttribute("verifyDto")) {
            VerifyOtpDto dto = new VerifyOtpDto();
            dto.setEmail(email);
            model.addAttribute("verifyDto", dto);
        }
        return "auth/verify-otp";
    }

    @PostMapping({"/verify-otp", "/auth/verify-otp"})
    public String verifyOtp(@Valid @ModelAttribute("verifyDto") VerifyOtpDto dto,
                            BindingResult result,
                            Model model,
                            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "auth/verify-otp";
        }
        try {
            authService.verifyOtp(dto.getEmail(), dto.getOtp());
            ra.addFlashAttribute("successMessage", "Xác thực tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("verifyDto", dto);
            return "auth/verify-otp";
        }
    }

    @PostMapping({"/resend-otp", "/auth/resend-otp"})
    public String resendOtp(@RequestParam("email") String email,
                            RedirectAttributes ra) {
        try {
            authService.resendOtp(email);
            ra.addFlashAttribute("successMessage", "Đã gửi lại mã xác thực OTP mới vào Gmail của bạn.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        ra.addAttribute("email", email);
        return "redirect:/verify-otp";
    }
}
