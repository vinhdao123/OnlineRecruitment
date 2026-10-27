package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.CompanyRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import OnlineRecruitment.OnlineRecruitment.service.ServicePackageService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;
    // Thêm 2 service này
    private final ServicePackageService servicePackageService;
    private final UserService userService;
    private final CompanyRepository companyRepository;

    public JobController(
            JobService jobService,
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            CvRepository cvRepository,
            ServicePackageService servicePackageService,
            UserService userService,
            CompanyRepository companyRepository) {

        this.jobService = jobService;
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.cvRepository = cvRepository;
        this.servicePackageService = servicePackageService;
        this.userService = userService;
        this.companyRepository = companyRepository;
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
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        List<Job> jobs = jobService.filterJobs(
                keyword, location, locations, experiences, salaryRanges, levels, categoryIds
        );

        if (companyId != null) {
            jobs = jobs.stream()
                    .filter(job -> job.getCompany() != null
                            && java.util.Objects.equals(job.getCompany().getId(), companyId))
                    .collect(java.util.stream.Collectors.toList());
        }

        if ("salary-high".equals(sort)) {
            jobs.sort(java.util.Comparator.comparing(
                    Job::getSalaryMax, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
        } else if ("salary-low".equals(sort)) {
            jobs.sort(java.util.Comparator.comparing(
                    Job::getSalaryMax, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        }

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
    public String showApplyPage(@PathVariable Long id, Model model) {
        Optional<Job> jobOptional = jobService.findJobById(id);
        if (jobOptional.isEmpty()) {
            return "redirect:/jobs";
        }
        model.addAttribute("job", jobOptional.get());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(username);
        }
        User user = userOptional.orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        Candidate candidate = candidateRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ ứng viên"));

        List<Cv> cvs = cvRepository.findByCandidateIdAndStatusOrderByIsDefaultDescUpdatedAtDesc(candidate.getId(), "ACTIVE");

        model.addAttribute("candidate", candidate);
        model.addAttribute("candidateName", user.getFullName());
        model.addAttribute("candidateEmail", user.getEmail());
        model.addAttribute("cvs", cvs);

        return "job/apply";
    }

    // ==========================================
    // HÀM MỚI: XỬ LÝ ĐĂNG BÀI CỦA RECRUITER
    // ==========================================
    @PostMapping("/recruiter/jobs/create")
    public String createJob(
            @ModelAttribute("job") Job job,
            Authentication authentication,
            RedirectAttributes ra) {

        try {
            String username = authentication.getName();
            User recruiter = userService.getByEmail(username);

            // 1. Lấy công ty của Recruiter
            Company company = companyRepository.findByRecruiterId(recruiter.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Tài khoản chưa có thông tin công ty. Vui lòng cập nhật công ty trước."));

            // 2. Kiểm tra gói dịch vụ
            UserPackage activePkg = servicePackageService.getActivePackage(recruiter);
            if (activePkg == null) {
                ra.addFlashAttribute("error", "Bạn chưa có gói dịch vụ đang hoạt động. Vui lòng mua gói để đăng bài!");
                return "redirect:/pricing";
            }

            // 3. Đếm số bài đã đăng và kiểm tra giới hạn
            long currentJobCount = jobService.countMyJobs(recruiter.getId());
            int maxJobs = activePkg.getServicePackage().getMaxJobs();
            if (maxJobs > 0 && currentJobCount >= maxJobs) {
                ra.addFlashAttribute("error", "Bạn đã đạt giới hạn " + maxJobs + " tin tuyển dụng của gói " +
                        activePkg.getServicePackage().getName() + ". Vui lòng nâng cấp gói!");
                return "redirect:/pricing";
            }

            // 4. Gán thông tin và lưu (Mở comment 2 dòng quan trọng này)
            job.setRecruiter(recruiter);
            job.setCompany(company); // Gán công ty vừa tìm được
            job.setStatus(JobStatus.PENDING); // Hoặc ACTIVE tùy vào quy trình duyệt của bạn

            jobService.saveJob(job);

            ra.addFlashAttribute("success", "Đăng tin tuyển dụng thành công!");
            return "redirect:/recruiter/jobs";

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/recruiter/jobs/create";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            return "redirect:/recruiter/jobs/create";
        }
    }
}