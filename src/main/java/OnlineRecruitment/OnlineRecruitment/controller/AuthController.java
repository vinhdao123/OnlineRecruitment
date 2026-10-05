package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.dto.CandidateRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.RecruiterRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.VerifyOtpDto;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ==========================================
    // 1. ĐĂNG NHẬP (LOGIN)
    // ==========================================
    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "verified", required = false) String verified,
            @RequestParam(value = "verifyError", required = false) String verifyError,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác, hoặc tài khoản chưa kích hoạt.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Bạn đã đăng xuất thành công.");
        }
        if (verified != null) {
            model.addAttribute("successMessage", "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
        }
        if (verifyError != null) {
            model.addAttribute("errorMessage", "Liên kết xác thực không hợp lệ hoặc đã hết hạn.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String redirectToCandidateRegister() {
        return "redirect:/register/candidate";
    }

    // ==========================================
    // 2. ĐĂNG KÝ CANDIDATE (SRS 3.2.2)
    // ==========================================
    @GetMapping("/register/candidate")
    public String showCandidateRegisterForm(Model model) {
        if (!model.containsAttribute("candidateDto")) {
            model.addAttribute("candidateDto", new CandidateRegisterDto());
        }
        return "auth/register-candidate";
    }

    @PostMapping("/register/candidate")
    public String registerCandidate(@Valid @ModelAttribute("candidate") CandidateRegisterDto dto,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register-candidate";
        }

        try {
            authService.registerCandidate(dto);
            // THÊM: Truyền email sang URL redirect để verify-otp.html đọc được
            redirectAttributes.addAttribute("email", dto.getEmail());
            redirectAttributes.addFlashAttribute("message", "Đăng ký thành công! Hệ thống đã gửi mã OTP.");
            return "redirect:/auth/verify-otp";
        } catch (Exception e) {
            result.rejectValue("email", "error.candidate", e.getMessage());
            return "auth/register-candidate";
        }
    }

    // ==========================================
    // 3. ĐĂNG KÝ RECRUITER (SRS 3.2.3)
    // ==========================================
    @GetMapping("/register/recruiter")
    public String showRecruiterRegisterForm(Model model) {
        if (!model.containsAttribute("recruiterDto")) {
            model.addAttribute("recruiterDto", new RecruiterRegisterDto());
        }
        return "auth/register-recruiter";
    }

    @PostMapping("/register/recruiter")
    public String processRecruiterRegister(
            @Valid @ModelAttribute("recruiterDto") RecruiterRegisterDto recruiterDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "auth/register-recruiter";
        }

        try {
            authService.registerRecruiter(recruiterDto);
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Đăng ký thành công! Hệ thống đã gửi mã OTP xác thực đến email: " + recruiterDto.getPersonalEmail());
            return "redirect:/auth/verify-otp?email=" + URLEncoder.encode(recruiterDto.getPersonalEmail(), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-recruiter";
        } catch (Exception e) {
            log.error("Lỗi đăng ký recruiter: ", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!");
            return "auth/register-recruiter";
        }
    }

    // ==========================================
    // 4. XÁC THỰC OTP QUA GMAIL
    // ==========================================
    @GetMapping("/auth/verify-otp")
    public String showVerifyOtpForm(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "resent", required = false) String resent,
            Model model) {

        VerifyOtpDto verifyDto = new VerifyOtpDto();
        if (email != null) {
            verifyDto.setEmail(email);
        }
        model.addAttribute("verifyDto", verifyDto);

        if (resent != null) {
            model.addAttribute("successMessage", "Mã xác thực mới đã được gửi lại vào email của bạn!");
        }

        return "auth/verify-otp";
    }

    @PostMapping("/auth/verify-otp")
    public String processVerifyOtp(
            @Valid @ModelAttribute("verifyDto") VerifyOtpDto verifyDto,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/verify-otp";
        }

        boolean verified = authService.verifyEmailOtp(verifyDto.getEmail(), verifyDto.getOtp());
        if (verified) {
            return "redirect:/login?verified=true";
        } else {
            model.addAttribute("errorMessage", "Mã xác thực không chính xác hoặc đã hết hạn. Vui lòng kiểm tra lại!");
            return "auth/verify-otp";
        }
    }

    @PostMapping("/auth/resend-otp")
    public String resendOtp(@RequestParam("email") String email, RedirectAttributes redirectAttributes) {
        try {
            authService.resendOtp(email);
            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addFlashAttribute("message", "Đã gửi lại mã OTP mới!");
        } catch (Exception e) {
            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addFlashAttribute("error", "Lỗi gửi lại OTP: " + e.getMessage());
        }
        return "redirect:/auth/verify-otp";
    }

    // ==========================================
    // 5. XÁC THỰC BẰNG LINK TRỰC TIẾP TỪ EMAIL
    // ==========================================
    @GetMapping("/auth/verify")
    public String verifyByDirectLink(@RequestParam("token") String token) {
        boolean verified = authService.verifyByToken(token);
        if (verified) {
            return "redirect:/login?verified=true";
        } else {
            return "redirect:/login?verifyError=true";
        }
    }
}
