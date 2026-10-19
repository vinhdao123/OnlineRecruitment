package OnlineRecruitment.OnlineRecruitment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Trang chủ: http://localhost:8080/
    @GetMapping("/")
    public String home() {
        return "home";
    }

    // Trang giới thiệu: http://localhost:8080/about
    @GetMapping("/about")
    public String about() {
        return "about";
    }

    // Trang công ty: tạm chuyển đến danh sách việc làm
    @GetMapping("/companies")
    public String companies() {
        return "redirect:/jobs";
    }

    // Trang dành cho ứng viên

    // Trang tổng quan Recruiter: chuyển đến trang quản lý tin
    @GetMapping("/recruiter/dashboard")
    public String recruiterDashboard() {
        return "redirect:/recruiter/jobs";
    }

    // Trang lỗi không có quyền truy cập
    @GetMapping("/403")
    public String forbidden() {
        return "403";
    }
}
