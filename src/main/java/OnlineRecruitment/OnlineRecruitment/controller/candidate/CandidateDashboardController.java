package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.service.JobService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// Trang việc làm cho ứng viên: GET /candidate/dashboard
// Giống trang quản lý của recruiter nhưng CHỈ XEM (không sửa/đóng/mở tin),
// và chỉ hiện các tin đang tuyển (ACTIVE).
// (SecurityConfig đã giới hạn /candidate/** chỉ cho role CANDIDATE)
// LƯU Ý: nếu project đã có controller khác map "/candidate/dashboard"
// thì xóa mapping cũ đó, nếu không sẽ lỗi "Ambiguous mapping".
@Controller
@RequiredArgsConstructor
public class CandidateDashboardController {

    // Số tin hiển thị trên mỗi trang
    private static final int PAGE_SIZE = 9;

    private final JobService jobService;

    @GetMapping("/candidate/dashboard")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        // Lấy các tin đang tuyển (ACTIVE, công ty ACTIVE, chưa hết hạn),
        // lọc theo từ khóa và địa điểm, mới nhất lên đầu
        List<Job> allJobs = jobService.filterJobs(
                keyword,
                location,
                null,   // danh sách địa điểm
                null,   // kinh nghiệm
                null,   // mức lương
                null,   // cấp bậc
                null    // danh mục
        );

        // Tính số trang (luôn có ít nhất 1 trang)
        int totalFound = allJobs.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalFound / (double) PAGE_SIZE));

        // Giữ số trang hiện tại trong khoảng hợp lệ
        int currentPage = Math.min(Math.max(page, 0), totalPages - 1);

        // Cắt đúng các tin của trang hiện tại
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, totalFound);
        List<Job> pageContent = allJobs.subList(from, to);

        // Đóng gói thành Page để giao diện dùng
        Page<Job> jobPage = new PageImpl<>(
                pageContent,
                PageRequest.of(currentPage, PAGE_SIZE),
                totalFound
        );

        // Chỉ hiện tối đa 5 nút số trang quanh trang hiện tại
        int pageStart = Math.max(0, currentPage - 2);
        int pageEnd = Math.min(totalPages - 1, currentPage + 2);

        model.addAttribute("jobs", pageContent);
        model.addAttribute("jobPage", jobPage);
        model.addAttribute("pageStart", pageStart);
        model.addAttribute("pageEnd", pageEnd);
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);

        // File: templates/candidate/dashboard.html
        return "candidate/dashboard";
    }
}