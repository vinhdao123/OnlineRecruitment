package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.dto.auth.VerifyOtpDto;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

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

    // 1. Hiển thị trang Quên mật khẩu
    @GetMapping({"/forgot-password", "/auth/forgot-password"})
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    // 2. Gửi mã OTP quên mật khẩu về Gmail
    @PostMapping({"/forgot-password/send-otp", "/auth/forgot-password/send-otp"})
    public String sendForgotPasswordOtp(@RequestParam("email") String email,
                                        RedirectAttributes ra) {
        try {
            authService.sendForgotPasswordOtp(email);
            ra.addFlashAttribute("successMessage", "Mã xác thực OTP đã được gửi đến hộp thư Gmail: " + email);
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("inputEmail", email);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("inputEmail", email);
        }
        return "redirect:/forgot-password";
    }

    // 3. Đặt lại mật khẩu bằng OTP
    @PostMapping({"/forgot-password/reset", "/auth/forgot-password/reset"})
    public String resetPassword(@RequestParam("email") String email,
                                @RequestParam("otp") String otp,
                                @RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                RedirectAttributes ra) {
        try {
            authService.resetPasswordWithOtp(email, otp, newPassword, confirmPassword);
            ra.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("otpSent", true);
            ra.addFlashAttribute("inputEmail", email);
            ra.addFlashAttribute("inputOtp", otp);
            return "redirect:/forgot-password";
        }
    }

    // --- JSON API cho giao diện wizard từng bước ---
    @PostMapping("/forgot-password/api/send-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiSendOtp(@RequestParam("email") String email) {
        try {
            authService.sendForgotPasswordOtp(email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Mã xác thực OTP đã được gửi đến hộp thư Gmail: " + email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/api/verify-otp")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiVerifyOtp(@RequestParam("email") String email,
                                                            @RequestParam("otp") String otp) {
        try {
            authService.validateOtpOnly(email, otp);
            return ResponseEntity.ok(Map.of("success", true, "message", "Mã xác thực OTP chính xác!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password/api/reset")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiResetPassword(@RequestParam("email") String email,
                                                                @RequestParam("otp") String otp,
                                                                @RequestParam("newPassword") String newPassword,
                                                                @RequestParam("confirmPassword") String confirmPassword) {
        try {
            authService.resetPasswordWithOtp(email, otp, newPassword, confirmPassword);
            return ResponseEntity.ok(Map.of("success", true, "message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
