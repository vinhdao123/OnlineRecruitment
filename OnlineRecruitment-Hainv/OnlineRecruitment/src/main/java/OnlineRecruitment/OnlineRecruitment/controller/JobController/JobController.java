package OnlineRecruitment.OnlineRecruitment.controller.JobController;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.JobApplication;
import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.respositoryJob.JobApplicationRepository;
import OnlineRecruitment.OnlineRecruitment.service.serviceJob.JobService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;

    public JobController(
            JobService jobService,
            UserRepository userRepository,
            JobApplicationRepository applicationRepository) {

        this.jobService = jobService;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    // ===== DANH SÁCH VIỆC LÀM CÔNG KHAI =====

    @GetMapping("/jobs")
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Page<Job2> jobPage =
                jobService.search(keyword, location, page);

        model.addAttribute("jobPage", jobPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("locations", jobService.getLocations());

        // File: templates/Job/jobs.html
        return "Job/jobs";
    }

    // ===== CHI TIẾT VIỆC LÀM =====

    @GetMapping("/jobs/{id}")
    public String detail(
            @PathVariable Long id,
            Model model,
            @AuthenticationPrincipal UserDetails principal,
            Authentication authentication) {

        Job2 job = jobService.getById(id);

        // Lấy ID người dùng đang đăng nhập từ email.
        // Khách chưa đăng nhập sẽ có userId = null.
        Long userId = null;

        if (principal != null) {
            userId = userRepository.findByEmail(principal.getUsername())
                    .map(User::getId)
                    .orElse(null);
        }

        // Kiểm tra quyền Admin.
        boolean isAdmin = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));

        // Tin chưa duyệt, bị từ chối hoặc đã đóng:
        // chỉ chủ tin hoặc Admin được xem.
        if (!"APPROVED".equals(job.getStatus())
                && !jobService.canManage(job, userId)
                && !isAdmin) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy việc làm."
            );
        }

        // Lấy username của người đăng tin.
        String recruiterUsername =
                userRepository.findById(job.getRecruiterId())
                        .map(User::getUsername)
                        .orElse("Không xác định");

        // ===== KIỂM TRA HỒ SƠ ỨNG TUYỂN =====

        boolean hasApplied = false;
        String applicationStatus = null;

        if (userId != null) {

            // Lấy danh sách hồ sơ của tài khoản hiện tại
            // cho đúng công việc đang xem.
            List<JobApplication> applications =
                    applicationRepository
                            .findApplicationsByUserIdAndJobId(
                                    userId,
                                    job.getId()
                            );

            // Danh sách đã sắp xếp theo appliedAt giảm dần.
            // Phần tử đầu tiên là hồ sơ mới nhất.
            if (applications != null && !applications.isEmpty()) {
                JobApplication latestApplication = applications.get(0);

                hasApplied = true;
                applicationStatus = latestApplication.getStatus();
            }
        }

        // ===== ĐƯA DỮ LIỆU SANG TRANG HTML =====

        model.addAttribute("job", job);
        model.addAttribute("recruiterUsername", recruiterUsername);

        model.addAttribute("hasApplied", hasApplied);
        model.addAttribute("applicationStatus", applicationStatus);

        // Quyền chỉnh sửa và duyệt tin.
        model.addAttribute(
                "canEdit",
                userId != null && jobService.canEdit(job, userId)
        );

        model.addAttribute(
                "canReview",
                isAdmin && "PENDING".equals(job.getStatus())
        );

        // File: templates/Job/job-detail.html
        return "Job/job-detail";
    }
}
