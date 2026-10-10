package OnlineRecruitment.OnlineRecruitment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Trang chủ
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // Chuyển đến đăng nhập trước khi đăng tin tuyển dụng
    @GetMapping("/employer/post-job")
    public String postJob() {
        return "redirect:/login";
    }

    // Trang dành cho nhà tuyển dụng
    @GetMapping("/employer")
    public String employer() {
        return "redirect:/recruiter/jobs";
    }

    // Trang không có quyền truy cập
    @GetMapping("/403")
    public String forbidden() {
        return "403";
    }
}