package OnlineRecruitment.OnlineRecruitment.controller.Recruiter;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.InterviewRepository;
import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.RecruiterWishlistRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class RecruiterWishlistController {

    private final RecruiterWishlistRepository wishlistRepository;
    private final InterviewRepository interviewRepository;

    public RecruiterWishlistController(
            RecruiterWishlistRepository wishlistRepository,
            InterviewRepository interviewRepository) {

        this.wishlistRepository = wishlistRepository;
        this.interviewRepository = interviewRepository;
    }

    // Hiển thị danh sách ứng viên đã lưu
    @GetMapping("/recruiter/wishlist")
    public String showWishlist(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long recruiterId =
                interviewRepository.findRecruiterId(
                        authentication.getName()
                );

        if (recruiterId == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "wishlist",
                wishlistRepository.findWishlistDetails(recruiterId)
        );

        return "recruiter/wishlist";
    }

    // Thêm hoặc xóa ứng viên khỏi Wishlist
    @PostMapping("/recruiter/wishlist/toggle")
    public String toggleWishlist(
            @RequestParam("candidateId") Long candidateId,
            @RequestParam("jobId") Long jobId,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(
                    value = "returnToWishlist",
                    defaultValue = "false"
            ) boolean returnToWishlist,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        Long recruiterId =
                interviewRepository.findRecruiterId(
                        authentication.getName()
                );

        if (recruiterId == null) {
            return "redirect:/login";
        }

        boolean alreadySaved =
                wishlistRepository.exists(
                        recruiterId,
                        candidateId,
                        jobId
                );

        if (alreadySaved) {

            wishlistRepository.removeWishlist(
                    recruiterId,
                    candidateId,
                    jobId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa ứng viên khỏi Wishlist."
            );

        } else {

            int result = wishlistRepository.addWishlist(
                    recruiterId,
                    candidateId,
                    jobId
            );

            if (result > 0) {
                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Đã thêm ứng viên vào Wishlist."
                );
            } else {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Không thể thêm ứng viên vào Wishlist."
                );
            }
        }

        // Thao tác từ trang Wishlist: quay lại Wishlist
        if (returnToWishlist) {
            return "redirect:/recruiter/wishlist";
        }

        // Thao tác từ trang lịch phỏng vấn: quay lại lịch phỏng vấn
        if (date == null || date.trim().isEmpty()) {
            date = LocalDate.now().toString();
        }

        return "redirect:/recruiter/interviews?date=" + date;
    }
}
