package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterRegisterDto;
import OnlineRecruitment.OnlineRecruitment.dto.Recruiter.RecruiterResubmitDto;
import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.service.AuthService;
import OnlineRecruitment.OnlineRecruitment.service.CompanyService;
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
    private final CompanyService companyService;

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

    // 1. Hiển thị form chỉnh sửa hồ sơ bị từ chối
    @GetMapping("/register/recruiter/resubmit")
    public String showResubmitForm(@RequestParam("email") String email,
                                   @RequestParam("token") String token,
                                   Model model) {
        try {
            Company company = companyService.getCompanyForResubmit(email, token);

            RecruiterResubmitDto dto = new RecruiterResubmitDto();
            if (company.getRecruiter() != null) {
                dto.setFullName(company.getRecruiter().getFullName());
                dto.setPhone(company.getRecruiter().getPhone());
            }
            dto.setCompanyName(company.getCompanyName());
            dto.setTaxCode(company.getTaxCode());
            dto.setCompanyEmail(company.getEmail());
            dto.setCompanyPhone(company.getPhone());
            dto.setCompanyAddress(company.getAddress());
            dto.setCompanyWebsite(company.getWebsite());
            dto.setCompanySize(company.getCompanySize());
            dto.setCompanyDescription(company.getDescription());

            model.addAttribute("resubmitDto", dto);
            model.addAttribute("company", company);
            model.addAttribute("email", email);
            model.addAttribute("token", token);
            model.addAttribute("rejectionReason", company.getRejectionReason());

            return "auth/resubmit-recruiter";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/resubmit-recruiter";
        }
    }

    // 2. Tiếp nhận hồ sơ sửa lại và gửi lại cho Admin duyệt
    @PostMapping("/register/recruiter/resubmit")
    public String handleResubmit(@RequestParam("email") String email,
                                 @RequestParam("token") String token,
                                 @Valid @ModelAttribute("resubmitDto") RecruiterResubmitDto dto,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes ra) {
        if (result.hasErrors()) {
            try {
                Company company = companyService.getCompanyForResubmit(email, token);
                model.addAttribute("company", company);
                model.addAttribute("email", email);
                model.addAttribute("token", token);
                model.addAttribute("rejectionReason", company.getRejectionReason());
            } catch (Exception ignored) {}
            return "auth/resubmit-recruiter";
        }

        try {
            companyService.resubmitCompany(email, token, dto);
            ra.addFlashAttribute("successMessage", 
                    "Hồ sơ doanh nghiệp của bạn đã được cập nhật và gửi lại thành công! Quản trị viên sẽ sớm xem xét và phê duyệt.");
            return "redirect:/login?resubmitSuccess=true";
        } catch (Exception e) {
            try {
                Company company = companyService.getCompanyForResubmit(email, token);
                model.addAttribute("company", company);
                model.addAttribute("email", email);
                model.addAttribute("token", token);
                model.addAttribute("rejectionReason", company.getRejectionReason());
            } catch (Exception ignored) {}
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/resubmit-recruiter";
        }
    }
}
