package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.service.WishlistService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

// =====================================================
// CONTROLLER WISHLIST CỦA ỨNG VIÊN
//
// Controller chỉ làm nhiệm vụ:
//   - Lấy tài khoản đang đăng nhập
//   - Gọi WishlistService xử lý nghiệp vụ
//   - Đưa thông báo / dữ liệu ra giao diện
// Toàn bộ logic nằm ở WishlistService,
// truy vấn DB nằm ở WishlistRepository.
// =====================================================

@Controller
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    // ==========================================
    // LẤY CANDIDATE ĐANG ĐĂNG NHẬP
    // Chưa đăng nhập / anonymous -> Optional rỗng
    // ==========================================

    private Optional<Candidate> getCurrentCandidate() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {

            return Optional.empty();
        }

        return wishlistService.findCandidateByLoginName(authentication.getName());
    }

    // ==========================================
    // THÊM / XÓA CÔNG VIỆC YÊU THÍCH
    //
    // Luồng:
    // 1. Kiểm tra đăng nhập -> chưa có Candidate thì về /login
    // 2. Gọi service bật / tắt yêu thích
    // 3. Dựa vào kết quả để gắn thông báo, quay về /jobs
    // ==========================================

    @PostMapping("/candidate/wishlist/toggle/{jobId}")
    public String toggleWishlist(
            @PathVariable Long jobId,
            RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra đăng nhập
        Optional<Candidate> candidateOptional = getCurrentCandidate();

        if (candidateOptional.isEmpty()) {
            return "redirect:/login";
        }

        // 2. Thêm hoặc xóa khỏi wishlist
        WishlistService.ToggleResult result =
                wishlistService.toggleWishlist(candidateOptional.get().getId(), jobId);

        // 3. Gắn thông báo theo kết quả
        switch (result) {
            case JOB_NOT_FOUND -> redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Công việc không tồn tại!"
            );
            case REMOVED -> redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã bỏ công việc khỏi danh sách yêu thích!"
            );
            case ADDED -> redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm công việc vào danh sách yêu thích!"
            );
        }

        return "redirect:/jobs";
    }

    // ==========================================
    // XEM DANH SÁCH CÔNG VIỆC YÊU THÍCH
    //
    // Luồng:
    // 1. Kiểm tra đăng nhập -> chưa có Candidate thì về /login
    // 2. Gọi service lấy danh sách Job đã lưu
    // 3. Đưa danh sách và tổng số lên giao diện
    // ==========================================

    @GetMapping("/candidate/wishlist")
    public String showWishlist(Model model) {

        // 1. Kiểm tra Candidate đăng nhập
        Optional<Candidate> candidateOptional = getCurrentCandidate();

        if (candidateOptional.isEmpty()) {
            return "redirect:/login";
        }

        // 2. Lấy danh sách công việc yêu thích
        List<Job> wishlistJobs =
                wishlistService.getWishlistJobs(candidateOptional.get().getId());

        // 3. Đưa dữ liệu lên giao diện
        model.addAttribute("wishlistJobs", wishlistJobs);
        model.addAttribute("totalWishlist", wishlistJobs.size());

        return "candidate/wishlist";
    }

}
