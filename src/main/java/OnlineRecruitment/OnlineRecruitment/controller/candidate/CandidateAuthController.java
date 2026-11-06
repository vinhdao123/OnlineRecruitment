package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.dto.candidate.CandidateRegisterDto;
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
public class CandidateAuthController {

    private final AuthService authService;

    @GetMapping("/register/candidate")
    public String showRegisterForm(Model model) {
        model.addAttribute("candidateDto", new CandidateRegisterDto());
        return "auth/register-candidate";
    }

    @PostMapping("/register/candidate")
    public String register(
            @Valid @ModelAttribute("candidateDto") CandidateRegisterDto dto,
            BindingResult result,
            Model model,
            RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "auth/register-candidate";
        }
        try {
            authService.registerCandidate(dto);
            ra.addAttribute("email", dto.getEmail());
            return "redirect:/verify-otp";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register-candidate";
        }
    }
}