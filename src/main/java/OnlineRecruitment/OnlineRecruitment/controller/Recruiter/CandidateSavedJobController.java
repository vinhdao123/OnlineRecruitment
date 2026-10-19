package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.entity.SavedJob;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SavedJobRepository;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Tin đã lưu của ứng viên: xem danh sách, lưu / bỏ lưu một tin
// (/candidate/** đã được SecurityConfig giới hạn cho role CANDIDATE)
@Controller
@RequiredArgsConstructor
public class CandidateSavedJobController {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    // ================= DANH SÁCH TIN ĐÃ LƯU =================
    @GetMapping("/candidate/saved")
    @Transactional(readOnly = true)
    public String list(
            @RequestParam(defaultValue = "open") String tab,
            Authentication authentication,
            Model model
    ) {
        User user = currentUser(authentication);

        // Lấy toàn bộ tin đã lưu rồi tách thành 2 nhóm: đang tuyển / đã đóng
        List<Job> all = savedJobRepository.findByUserWithJob(user.getId())
                .stream().map(SavedJob::getJob).toList();

        List<Job> open = all.stream().filter(j -> j.getStatus() == JobStatus.ACTIVE).toList();
        List<Job> closed = all.stream().filter(j -> j.getStatus() != JobStatus.ACTIVE).toList();

        boolean closedTab = "closed".equalsIgnoreCase(tab);

        model.addAttribute("mode", "candidate");
        model.addAttribute("pageHeading", "Tin đã lưu");
        model.addAttribute("pageSub", "Những tin bạn đã đánh dấu để xem lại hoặc ứng tuyển sau.");
        model.addAttribute("activeTab", closedTab ? "closed" : "open");
        model.addAttribute("tabOpenCount", open.size());
        model.addAttribute("tabClosedCount", closed.size());
        model.addAttribute("jobs", closedTab ? closed : open);

        // File: templates/job/library.html
        return "job/library";
    }

    // ================= LƯU / BỎ LƯU (bấm lần 1 lưu, lần 2 bỏ lưu) =================
    @PostMapping("/candidate/saved/{jobId}/toggle")
    @Transactional
    public String toggle(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "detail") String from,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        User user = currentUser(authentication);

        var existing = savedJobRepository.findByUser_IdAndJob_Id(user.getId(), jobId);

        if (existing.isPresent()) {
            // Đã lưu rồi -> bỏ lưu
            savedJobRepository.delete(existing.get());
            redirectAttributes.addFlashAttribute("successMessage", "Đã bỏ lưu tin.");
        } else {
            // Chưa lưu -> kiểm tra tin tồn tại và đang tuyển rồi mới lưu
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null || job.getStatus() != JobStatus.ACTIVE) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ lưu được tin đang tuyển.");
            } else {
                SavedJob saved = new SavedJob();
                saved.setUser(user);
                saved.setJob(job);
                savedJobRepository.save(saved);
                redirectAttributes.addFlashAttribute("successMessage", "Đã lưu tin.");
            }
        }

        // Quay lại đúng trang người dùng vừa bấm (trang lưu hoặc trang chi tiết)
        return "saved".equals(from)
                ? "redirect:/candidate/saved"
                : "redirect:/jobs/" + jobId;
    }

    // Lấy tài khoản đang đăng nhập (đăng nhập bằng email, giống các controller khác)
    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đang đăng nhập."));
    }
}