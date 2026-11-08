
package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.service.JobService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class WishlistController {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobService jobService;

    public WishlistController(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            JobService jobService) {

        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.jobService = jobService;
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

        User user = userOptional.get();

        return candidateRepository.findByUserId(user.getId());
    }

    // ==========================================
    // THÊM / XÓA CÔNG VIỆC YÊU THÍCH
    // ==========================================

    @PostMapping("/candidate/wishlist/toggle/{jobId}")
    @Transactional
    public String toggleWishlist(
            @PathVariable Long jobId,
            RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập

        Optional<Candidate> candidateOptional =
                getCurrentCandidate();

        if (candidateOptional.isEmpty()) {
            return "redirect:/login";
        }

        Long candidateId =
                candidateOptional.get().getId();

        // 2. Kiểm tra công việc có tồn tại không

        Optional<Job> jobOptional =
                jobService.findJobById(jobId);

        if (jobOptional.isEmpty()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Công việc không tồn tại!"
            );

            return "redirect:/jobs";
        }

        // 3. Kiểm tra công việc đã được yêu thích chưa

        String checkSql = """
                SELECT COUNT(*)
                FROM dbo.wishlist
                WHERE candidate_id = ?
                  AND job_id = ?
                """;

        Integer count = jdbcTemplate.queryForObject(
                checkSql,
                Integer.class,
                candidateId,
                jobId
        );

        // 4. Nếu đã tồn tại thì xóa

        if (count != null && count > 0) {

            String deleteSql = """
                    DELETE FROM dbo.wishlist
                    WHERE candidate_id = ?
                      AND job_id = ?
                    """;

            jdbcTemplate.update(
                    deleteSql,
                    candidateId,
                    jobId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã bỏ công việc khỏi danh sách yêu thích!"
            );

        } else {

            // 5. Nếu chưa tồn tại thì thêm mới

            String insertSql = """
                    INSERT INTO dbo.wishlist
                    (candidate_id, job_id)
                    VALUES (?, ?)
                    """;

            jdbcTemplate.update(
                    insertSql,
                    candidateId,
                    jobId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm công việc vào danh sách yêu thích!"
            );
        }

        return "redirect:/jobs";
    }

    // ==========================================
    // XEM DANH SÁCH CÔNG VIỆC YÊU THÍCH
    // ==========================================

    @GetMapping("/candidate/wishlist")
    public String showWishlist(Model model) {

        // 1. Kiểm tra Candidate đăng nhập

        Optional<Candidate> candidateOptional =
                getCurrentCandidate();

        if (candidateOptional.isEmpty()) {
            return "redirect:/login";
        }

        Long candidateId =
                candidateOptional.get().getId();

        // 2. Lấy danh sách ID công việc đã lưu

        String sql = """
                SELECT job_id
                FROM dbo.wishlist
                WHERE candidate_id = ?
                ORDER BY created_at DESC
                """;

        List<Long> jobIds = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("job_id"),
                candidateId
        );

        // 3. Lấy thông tin chi tiết từng công việc

        List<Job> wishlistJobs = new ArrayList<>();

        for (Long jobId : jobIds) {

            Optional<Job> jobOptional =
                    jobService.findJobById(jobId);

            jobOptional.ifPresent(wishlistJobs::add);
        }

        // 4. Đưa dữ liệu lên giao diện

        model.addAttribute("wishlistJobs", wishlistJobs);

        model.addAttribute(
                "totalWishlist",
                wishlistJobs.size()
        );

        return "candidate/wishlist";
    }

}
