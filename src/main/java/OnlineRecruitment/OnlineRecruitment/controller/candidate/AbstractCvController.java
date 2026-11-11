package OnlineRecruitment.OnlineRecruitment.controller.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Cv;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Lớp cha dùng chung cho các controller của module CV (/candidate/cvs/**):
 * lấy userId đang đăng nhập, đưa currentUser vào model cho taskbar, và helper cho các trang tiểu mục.
 */
public abstract class AbstractCvController {

    protected final UserRepository userRepository;

    protected AbstractCvController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Tìm user đang đăng nhập theo email hoặc username. */
    protected User getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .or(() -> userRepository.findByUsername(auth.getName()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    }

    /** Lấy id của user đang đăng nhập. */
    protected Long getUserId(Authentication auth) {
        return getAuthenticatedUser(auth).getId();
    }

    /** Taskbar dùng chung cần currentUser (họ tên, avatar) trên mọi trang của module CV. */
    @ModelAttribute("currentUser")
    public User populateCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByEmail(auth.getName())
                    .or(() -> userRepository.findByUsername(auth.getName()))
                    .orElse(null);
        }
        return null;
    }

    /** Hiển thị trang quản lý một tiểu mục của CV (kinh nghiệm, học vấn, chứng chỉ...). */
    protected <T> String showSection(Long cvId, Authentication auth, Model model, RedirectAttributes ra,
                                     Function<Long, Cv> cvFetcher,
                                     String listAttr, Function<Long, List<T>> itemsFetcher,
                                     String reqAttr, Object emptyReq, String viewName,
                                     Runnable extraPopulate) {
        try {
            Long userId = getUserId(auth);
            model.addAttribute("cv", cvFetcher.apply(userId));
            try {
                model.addAttribute(listAttr, itemsFetcher.apply(userId));
            } catch (Exception ex) {
                model.addAttribute(listAttr, Collections.emptyList());
            }
            if (extraPopulate != null) {
                extraPopulate.run();
            }
            model.addAttribute(reqAttr, emptyReq);
            return viewName;
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy CV hoặc không có quyền truy cập: " + e.getMessage());
            return "redirect:/candidate/cvs";
        }
    }

    /** Xoá một mục con và chuyển hướng về trang tiểu mục tương ứng. */
    protected String deleteSectionItem(Long cvId, RedirectAttributes ra, Runnable action,
                                       String successMessage, String subPath) {
        try {
            action.run();
            ra.addFlashAttribute("successMessage", successMessage);
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/candidate/cvs/" + cvId + subPath;
    }

    /** Xử lý nhẹ nhàng khi file tải lên vượt quá giới hạn multipart của server. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("errorMessage", "File tải lên vượt quá dung lượng cho phép (tối đa 5MB cho CV, 2MB cho ảnh)!");
        return "redirect:/candidate/cvs";
    }
}
