package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SavedJobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

// Trang chi tiết một tin tuyển dụng: GET /jobs/{id}
// Được mở khi bấm nút "Xem" hoặc bấm vào tiêu đề tin
@Controller
@RequiredArgsConstructor
public class JobDetailController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final SavedJobRepository savedJobRepository;

    @GetMapping("/jobs/{id}")
    public String detail(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        // 1) Tìm tin theo id, không có thì báo 404
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy tin tuyển dụng."
                ));

        // 2) Kiểm tra đã đăng nhập chưa
        // (khách chưa đăng nhập vẫn có đối tượng Authentication kiểu "anonymous")
        boolean loggedIn = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        // 3) Có phải admin không
        boolean isAdmin = loggedIn
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        // 4) Người đang đăng nhập (đăng nhập bằng email)
        User currentUser = loggedIn
                ? userRepository.findByEmail(authentication.getName()).orElse(null)
                : null;

        // 5) Có phải chủ của tin này không
        boolean isOwner = currentUser != null
                && job.getRecruiter() != null
                && currentUser.getId().equals(job.getRecruiter().getId());

        // 6) Tin chưa được duyệt (nháp, chờ duyệt, bị từ chối, đã đóng)
        //    chỉ chủ tin hoặc admin được xem, người khác coi như không tồn tại
        if (job.getStatus() != JobStatus.ACTIVE && !isOwner && !isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tin tuyển dụng."
            );
        }

        // 7) Gửi dữ liệu sang trang HTML
        model.addAttribute("job", job);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("isAdmin", isAdmin);

        // 8) Ứng viên này đã lưu tin chưa (để nút hiện "Lưu tin" hay "Bỏ lưu")
        model.addAttribute("saved", currentUser != null
                && savedJobRepository.existsByUser_IdAndJob_Id(currentUser.getId(), job.getId()));

        // File: templates/job/job-detail.html
        return "job/job-detail";
    }
}