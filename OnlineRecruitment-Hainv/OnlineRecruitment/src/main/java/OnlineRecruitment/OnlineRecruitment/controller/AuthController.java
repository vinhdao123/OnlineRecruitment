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

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ==================== ĐĂNG NHẬP ====================

    @GetMapping("/login")
    public String showLoginForm(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "verified", required = false) String verified,
            @RequestParam(value = "verifyError", required = false) String verifyError,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage",
                    "Email hoặc mật khẩu không chính xác, hoặc tài khoản chưa kích hoạt.");
        }

        if (logout != null) {
            model.addAttribute("successMessage",
                    "Bạn đã đăng xuất thành công.");
        }

        if (verified != null) {
            model.addAttribute("successMessage",
                    "Xác thực email thành công! Bạn có thể đăng nhập ngay bây giờ.");
        }

        if (verifyError != null) {
            model.addAttribute("errorMessage",
                    "Liên kết xác thực không hợp lệ hoặc đã hết hạn.");
        }

        return "auth/login";
    }

    // ==================== TRANG ĐĂNG KÝ CHUNG ====================

    @GetMapping("/register")
    public String redirectToCandidateRegister() {
        return "redirect:/register/candidate";
    }

    // ==================== ĐĂNG KÝ CANDIDATE ====================

    @GetMapping("/register/candidate")
    public String showCandidateRegisterForm(Model model) {
        if (!model.containsAttribute("candidateDto")) {
            model.addAttribute("candidateDto", new CandidateRegisterDto());
        }

        return "auth/register-candidate";
    }

    @PostMapping("/register/candidate")
    public String registerCandidate(
            @Valid @ModelAttribute("candidateDto") CandidateRegisterDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra dữ liệu đầu vào
        if (result.hasErrors()) {
            log.warn("Đăng ký Candidate không hợp lệ: {}",
                    result.getFieldErrors());

            model.addAttribute("errorMessage",
                    "Vui lòng kiểm tra lại các thông tin đăng ký.");

            return "auth/register-candidate";
        }

        try {
            // Thực hiện đăng ký, lưu tài khoản và xử lý OTP
            authService.registerCandidate(dto);

            // Chỉ chuyển sang OTP khi service hoàn thành thành công
            redirectAttributes.addAttribute("email", dto.getEmail());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký thành công! Hệ thống đã gửi mã OTP đến email của bạn."
            );

            return "redirect:/auth/verify-otp";

        } catch (IllegalArgumentException e) {
            log.warn("Đăng ký Candidate thất bại: {}", e.getMessage());

            model.addAttribute(
                    "errorMessage",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Thông tin đăng ký không hợp lệ."
            );

            return "auth/register-candidate";

        } catch (Exception e) {
            log.error("Lỗi đăng ký Candidate", e);

            model.addAttribute(
                    "errorMessage",
                    "Đăng ký thất bại. Nguyên nhân: "
                            + (e.getMessage() != null
                            ? e.getMessage()
                            : "Vui lòng kiểm tra log ứng dụng.")
            );

            return "auth/register-candidate";
        }
    }

    // ==================== ĐĂNG KÝ RECRUITER ====================

    @GetMapping("/register/recruiter")
    public String showRecruiterRegisterForm(Model model) {
        if (!model.containsAttribute("recruiterDto")) {
            model.addAttribute("recruiterDto", new RecruiterRegisterDto());
        }

        return "auth/register-recruiter";
    }

    @PostMapping("/register/recruiter")
    public String processRecruiterRegister(
            @Valid @ModelAttribute("recruiterDto") RecruiterRegisterDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("errorMessage",
                    "Vui lòng kiểm tra lại các thông tin đăng ký.");

            return "auth/register-recruiter";
        }

        try {
            authService.registerRecruiter(dto);

            redirectAttributes.addAttribute("email", dto.getEmail());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đăng ký thành công! Hệ thống đã gửi mã OTP đến email của bạn."
            );

            return "redirect:/auth/verify-otp";

        } catch (IllegalArgumentException e) {
            log.warn("Đăng ký Recruiter thất bại: {}", e.getMessage());

            model.addAttribute(
                    "errorMessage",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Thông tin đăng ký không hợp lệ."
            );

            return "auth/register-recruiter";

        } catch (Exception e) {
            log.error("Lỗi đăng ký Recruiter", e);

            model.addAttribute(
                    "errorMessage",
                    "Đăng ký thất bại. Nguyên nhân: "
                            + (e.getMessage() != null
                            ? e.getMessage()
                            : "Vui lòng kiểm tra log ứng dụng.")
            );

            return "auth/register-recruiter";
        }
    }

    // ==================== HIỂN THỊ TRANG OTP ====================

    @GetMapping("/auth/verify-otp")
    public String showVerifyOtpForm(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "resent", required = false) String resent,
            Model model) {

        if (!model.containsAttribute("verifyDto")) {
            VerifyOtpDto dto = new VerifyOtpDto();
            dto.setEmail(email);
            model.addAttribute("verifyDto", dto);
        }

        if (resent != null) {
            model.addAttribute("successMessage",
                    "Mã xác thực mới đã được gửi lại vào email của bạn.");
        }

        return "auth/verify-otp";
    }

    // ==================== XỬ LÝ OTP ====================

    @PostMapping("/auth/verify-otp")
    public String processVerifyOtp(
            @Valid @ModelAttribute("verifyDto") VerifyOtpDto dto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("errorMessage",
                    "Vui lòng nhập email và mã OTP gồm 6 chữ số.");

            return "auth/verify-otp";
        }

        try {
            boolean verified = authService.verifyEmailOtp(
                    dto.getEmail(),
                    dto.getOtp()
            );

            if (verified) {
                return "redirect:/login?verified=true";
            }

            model.addAttribute("errorMessage",
                    "Mã OTP không chính xác hoặc đã hết hạn.");

            return "auth/verify-otp";

        } catch (Exception e) {
            log.error("Lỗi xác thực OTP", e);

            model.addAttribute("errorMessage",
                    "Không thể xác thực OTP. Vui lòng thử lại.");

            return "auth/verify-otp";
        }
    }

    // ==================== GỬI LẠI OTP ====================

    @PostMapping("/auth/resend-otp")
    public String resendOtp(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {

        try {
            authService.resendVerificationOtp(email);

            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addAttribute("resent", "true");

        } catch (Exception e) {
            log.error("Lỗi gửi lại OTP", e);

            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage() != null
                            ? e.getMessage()
                            : "Không thể gửi lại OTP. Vui lòng thử lại."
            );
        }

        return "redirect:/auth/verify-otp";
    }

    // ==================== XÁC THỰC BẰNG LINK ====================

    @GetMapping("/auth/verify")
    public String verifyByDirectLink(
            @RequestParam("token") String token) {

        try {
            boolean verified = authService.verifyByToken(token);

            if (verified) {
                return "redirect:/login?verified=true";
            }

        } catch (Exception e) {
            log.error("Lỗi xác thực bằng liên kết", e);
        }

        return "redirect:/login?verifyError=true";
    }
}
