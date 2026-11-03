
package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.User;

import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CvRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.WishlistRepository;

import OnlineRecruitment.OnlineRecruitment.service.JobService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
public class JobController {

    private final JobService jobService;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;
    private final WishlistRepository wishlistRepository;

    public JobController(
            JobService jobService,
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            CvRepository cvRepository,
            WishlistRepository wishlistRepository) {

        this.jobService = jobService;
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.cvRepository = cvRepository;
        this.wishlistRepository = wishlistRepository;
    }

    // ==========================================
    // LẤY CANDIDATE ĐANG ĐĂNG NHẬP
    // ==========================================

    private Optional<Candidate> getCurrentCandidate() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return Optional.empty();
        }

        String username = authentication.getName();

        Optional<User> userOptional =
                userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(username);
        }

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        return candidateRepository.findByUserId(
                userOptional.get().getId()
        );
    }

    // ==========================================
    // DANH SÁCH CÔNG VIỆC
    // ==========================================

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

            // Chỉ hiển thị công việc yêu thích
            @RequestParam(defaultValue = "false") boolean favoritesOnly,

            Model model) {

        // ==========================================
        // 1. LỌC CÔNG VIỆC
        // ==========================================

        List<Job> jobs = jobService.filterJobs(
                keyword,
                location,
                locations,
                experiences,
                salaryRanges,
                levels,
                categoryIds
        );

        // ==========================================
        // 2. LỌC THEO CÔNG TY
        // ==========================================

        if (companyId != null) {

            jobs = jobs.stream()
                    .filter(job ->
                            job.getCompany() != null
                                    && Objects.equals(
                                    job.getCompany().getId(),
                                    companyId
                            )
                    )
                    .collect(Collectors.toList());
        }

        // ==========================================
        // 3. LẤY DANH SÁCH JOB YÊU THÍCH
        // CỦA CANDIDATE ĐANG ĐĂNG NHẬP
        // ==========================================

        Set<Long> favoriteJobIds = new HashSet<>();

        Optional<Candidate> candidateOptional =
                getCurrentCandidate();

        if (candidateOptional.isPresent()) {

            Long candidateId =
                    candidateOptional.get().getId();

            favoriteJobIds.addAll(
                    wishlistRepository.findJobIdsByCandidateId(
                            candidateId
                    )
            );
        }

        // ==========================================
        // 4. CHỈ HIỂN THỊ JOB ĐÃ YÊU THÍCH
        // ==========================================

        if (favoritesOnly) {

            jobs.removeIf(job ->
                    !favoriteJobIds.contains(job.getId())
            );
        }

        // ==========================================
        // 5. SẮP XẾP CÔNG VIỆC
        // ==========================================

        if ("salary-high".equals(sort)) {

            jobs.sort(Comparator.comparing(
                    Job::getSalaryMax,
                    Comparator.nullsLast(
                            Comparator.reverseOrder()
                    )
            ));

        } else if ("salary-low".equals(sort)) {

            jobs.sort(Comparator.comparing(
                    Job::getSalaryMax,
                    Comparator.nullsLast(
                            Comparator.naturalOrder()
                    )
            ));
        }

        // ==========================================
        // 6. DỮ LIỆU CHO HTML
        // ==========================================

        model.addAttribute("jobs", jobs);

        model.addAttribute(
                "locationsList",
                jobService.getAllLocations()
        );

        model.addAttribute(
                "categories",
                jobService.getAllCategories()
        );

        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        model.addAttribute("locations", locations);
        model.addAttribute("experiences", experiences);
        model.addAttribute("salaryRanges", salaryRanges);
        model.addAttribute("levels", levels);
        model.addAttribute("categoryIds", categoryIds);
        model.addAttribute("sort", sort);
        model.addAttribute("companyId", companyId);

        // Dữ liệu Wishlist
        model.addAttribute("favoriteJobIds", favoriteJobIds);
        model.addAttribute("favoritesOnly", favoritesOnly);

        // ==========================================
        // 7. TRẢ VỀ TRANG JOB
        // ==========================================

        return "job/job";
    }

    // ==========================================
    // TRANG ỨNG TUYỂN
    // ==========================================

    @GetMapping("/jobs/{id}/apply")
    public String showApplyPage(
            @PathVariable Long id,
            Model model) {

        // 1. Lấy công việc

        Optional<Job> jobOptional =
                jobService.findJobById(id);

        if (jobOptional.isEmpty()) {
            return "redirect:/jobs";
        }

        model.addAttribute("job", jobOptional.get());

        // 2. Kiểm tra tài khoản đăng nhập

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return "redirect:/login";
        }

        // 3. Lấy thông tin User

        String username = authentication.getName();

        Optional<User> userOptional =
                userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(username);
        }

        User user = userOptional.orElseThrow(() ->
                new RuntimeException(
                        "Không tìm thấy tài khoản: " + username
                )
        );

        // 4. Tìm Candidate

        Candidate candidate = candidateRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Không tìm thấy hồ sơ ứng viên"
                        )
                );

        // 5. Lấy danh sách CV

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
