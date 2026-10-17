package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterRegisterDto;
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
public class RecruiterAuthController {

    private final AuthService authService;

    @GetMapping("/register/recruiter")
    public String showRegisterForm(Model model) {
        model.addAttribute("recruiterDto", new RecruiterRegisterDto());
        return "auth/register-recruiter";
    }

    @PostMapping("/register/recruiter")
    public String register(
            @Valid @ModelAttribute("recruiterDto") RecruiterRegisterDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "auth/register-recruiter";
        }
        try {
            authService.registerRecruiter(dto);
            ra.addAttribute("email", dto.getEmail());
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-recruiter";
        }
    }
}