package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Trang "Kho tin" của recruiter: tab Tin nháp và tab Tin đã đóng
// Bấm vào tin nháp sẽ mở lại form sửa với dữ liệu đã nhập trước đó
@Controller
@RequiredArgsConstructor
public class RecruiterJobArchiveController {

    private final JobService jobService;
    private final UserRepository userRepository;

    @GetMapping("/recruiter/jobs/archive")
    public String archive(
            @RequestParam(defaultValue = "draft") String tab,
            Authentication authentication,
            Model model
    ) {
        // 1) Người đang đăng nhập
        User recruiter = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập."));

        // 2) Chọn trạng thái theo tab (mặc định là tin nháp)
        boolean closedTab = "closed".equalsIgnoreCase(tab);
        JobStatus status = closedTab ? JobStatus.CLOSED : JobStatus.DRAFT;

        // 3) Lấy tin của chính recruiter này theo trạng thái
        List<Job> jobs = jobService.findMyJobs(recruiter.getId(), null, status.name());

        // 4) Đếm số tin ở từng tab để hiện trên nhãn tab
        long draftCount = jobService.countMyJobsByStatus(recruiter.getId(), JobStatus.DRAFT);
        long closedCount = jobService.countMyJobsByStatus(recruiter.getId(), JobStatus.CLOSED);

        model.addAttribute("mode", "recruiter");
        model.addAttribute("pageHeading", "Kho tin");
        model.addAttribute("pageSub", "Tin nháp và tin đã đóng của bạn. Bấm vào tin để làm tiếp, không cần nhập lại.");
        model.addAttribute("activeTab", closedTab ? "closed" : "draft");
        model.addAttribute("tabDraftCount", draftCount);
        model.addAttribute("tabClosedCount", closedCount);
        model.addAttribute("jobs", jobs);

        // File: templates/job/library.html
        return "job/library";
    }
}