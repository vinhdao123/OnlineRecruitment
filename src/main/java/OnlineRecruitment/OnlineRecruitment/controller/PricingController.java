package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.service.ServicePackageService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final ServicePackageService packageService;
    private final UserService userService;

    @GetMapping
    public String pricingPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        // Lấy thông tin user đang đăng nhập
        User user = userService.getByEmail(principal.getUsername());

        // Chỉ lấy các gói phù hợp với role hiện tại (RECRUITER hoặc CANDIDATE)
        model.addAttribute("packages", packageService.getPackagesForRole(user.getRole()));
        model.addAttribute("currentUser", user);

        return "pricing/index"; // Trả về file HTML
    }
}