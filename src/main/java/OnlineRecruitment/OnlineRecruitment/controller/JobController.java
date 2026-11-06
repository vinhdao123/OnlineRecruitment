package OnlineRecruitment.OnlineRecruitment.controller;



import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;

    public JobController(
            JobService jobService,
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            CvRepository cvRepository) {

        this.jobService = jobService;
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.cvRepository = cvRepository;
    }

    @GetMapping("/jobs")
    public String jobs(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) String location,

            @RequestParam(required = false) List<String> locations,

            @RequestParam(required = false) List<String> experiences,

            @RequestParam(required = false) List<String> salaryRanges,

            @RequestParam(required = false) List<String> levels,

            @RequestParam(required = false) List<Long> categoryIds,

// THÊM DÒNG NÀY
            @RequestParam(required = false) Long companyId,

            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        // ==========================================
        // LỌC
        // ==========================================

//        List<Job> jobs = jobService.filterJobs(
//                keyword,
//                location,
//                locations,
//                experiences,
//                salaryRanges,
//                levels,
//                categoryIds
//        );

        List<Job> jobs = jobService.filterJobs(
                keyword,
                location,
                locations,
                experiences,
                salaryRanges,
                levels,
                categoryIds
        );
        // Lọc thêm theo công ty nếu có companyId
        if (companyId != null) {
            jobs = jobs.stream()
                    .filter(job -> job.getCompany() != null
                            && java.util.Objects.equals(
                            job.getCompany().getId(),
                            companyId
                    ))
                    .collect(java.util.stream.Collectors.toList());
        }

        // ==========================================
        // SẮP XẾP
        // ==========================================

        if ("salary-high".equals(sort)) {
            jobs.sort(java.util.Comparator.comparing(
                    Job::getSalaryMax,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())
            ));
        } else if ("salary-low".equals(sort)) {
            jobs.sort(java.util.Comparator.comparing(
                    Job::getSalaryMax,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
            ));
        }

        // ==========================================
        // DỮ LIỆU CHO HTML
        // ==========================================

        model.addAttribute("jobs", jobs);
        model.addAttribute("locationsList", jobService.getAllLocations());
        model.addAttribute("categories", jobService.getAllCategories());

        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("locations", locations);
        model.addAttribute("experiences", experiences);
        model.addAttribute("salaryRanges", salaryRanges);
        model.addAttribute("levels", levels);
        model.addAttribute("categoryIds", categoryIds);
        model.addAttribute("sort", sort);
        model.addAttribute("companyId", companyId);

        return "job/job";
    }

    @GetMapping("/jobs/{id}/apply")
    public String showApplyPage(
            @PathVariable Long id,
            Model model) {

        // 1. Lấy công việc
        Optional<Job> jobOptional = jobService.findJobById(id);

        if (jobOptional.isEmpty()) {
            return "redirect:/jobs";
        }

        model.addAttribute("job", jobOptional.get());

        // 2. Lấy tài khoản đang đăng nhập
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        String username = authentication.getName();

        System.out.println("Gia tri dang dang nhap: " + username);

        Optional<User> userOptional =
                userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(username);
        }

        User user = userOptional.orElseThrow(() ->
                new RuntimeException("Không tìm thấy tài khoản: " + username));

        // 4. Tìm hồ sơ ứng viên
        Candidate candidate = candidateRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy hồ sơ ứng viên"));

        // 5. Lấy danh sách CV của ứng viên
        List<Cv> cvs = cvRepository
                .findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(
                        candidate.getId(),
                        "ACTIVE"
                );

        // 6. Đưa dữ liệu lên HTML
        model.addAttribute("candidate", candidate);
        model.addAttribute("candidateName", user.getFullName());
        model.addAttribute("candidateEmail", user.getEmail());
        model.addAttribute("cvs", cvs);

        return "job/apply";
    }


}