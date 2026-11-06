package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.dto.skilltest.QuestionAnswerItem;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestAttempt;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.service.SkillTestService;
import OnlineRecruitment.OnlineRecruitment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SkillTestController {

    private final SkillTestService skillTestService;
    private final UserService userService;

    // 1. Danh mục bài thi kỹ năng (Catalog như ảnh TopCV)
    @GetMapping("/skill-tests")
    public String listTests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {

        Page<SkillTest> testPage = skillTestService.getActiveTests(keyword, category, page, size);
        List<String> categories = skillTestService.getAllCategories();
        List<SkillTest> popularTests = skillTestService.getPopularTests();

        model.addAttribute("tests", testPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", testPage.getTotalPages());
        model.addAttribute("totalItems", testPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categories);
        model.addAttribute("popularTests", popularTests);
        model.addAttribute("activeTab", "skilltests");

        return "skilltest/list";
    }

    // 2. Chi tiết và hướng dẫn bài thi
    @GetMapping("/skill-tests/{id}")
    public String testDetail(@PathVariable Long id, Model model) {
        SkillTest test = skillTestService.getById(id);
        long questionCount = skillTestService.countQuestionsInPool(id);

        model.addAttribute("test", test);
        model.addAttribute("poolCount", questionCount);
        model.addAttribute("activeTab", "skilltests");

        return "skilltest/detail";
    }

    // 3. Giao diện làm bài thi (Random câu hỏi từ pool)
    @GetMapping("/skill-tests/{id}/take")
    public String takeTest(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails principal,
                           Model model,
                           RedirectAttributes ra) {
        if (principal == null) {
            ra.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để bắt đầu làm bài thi đánh giá kỹ năng!");
            return "redirect:/login";
        }

        try {
            SkillTest test = skillTestService.getById(id);
            List<SkillTestQuestion> randomQuestions = skillTestService.getRandomQuestionsForTest(id);

            model.addAttribute("test", test);
            model.addAttribute("questions", randomQuestions);
            model.addAttribute("totalQuestions", randomQuestions.size());
            model.addAttribute("durationMinutes", test.getDurationMinutes());
            model.addAttribute("activeTab", "skilltests");

            return "skilltest/take";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/skill-tests/" + id;
        }
    }

    // 4. Tiếp nhận nộp bài thi & chấm điểm
    @PostMapping("/skill-tests/{id}/submit")
    public String submitTest(@PathVariable Long id,
                             @RequestParam Map<String, String> allParams,
                             @RequestParam(value = "timeSpentSeconds", defaultValue = "0") int timeSpentSeconds,
                             @AuthenticationPrincipal UserDetails principal,
                             RedirectAttributes ra) {
        if (principal == null) {
            return "redirect:/login";
        }

        try {
            User user = userService.getByEmail(principal.getUsername());
            SkillTestAttempt attempt = skillTestService.submitTest(id, user, allParams, timeSpentSeconds);
            return "redirect:/skill-tests/result/" + attempt.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi khi nộp bài thi: " + e.getMessage());
            return "redirect:/skill-tests/" + id;
        }
    }

    // 5. Trang xem kết quả bài thi chi tiết
    @GetMapping("/skill-tests/result/{attemptId}")
    public String testResult(@PathVariable Long attemptId, Model model) {
        SkillTestAttempt attempt = skillTestService.getAttemptById(attemptId);
        List<QuestionAnswerItem> reviewItems = skillTestService.parseReviewItems(attempt.getAnswersJson());

        model.addAttribute("attempt", attempt);
        model.addAttribute("test", attempt.getSkillTest());
        model.addAttribute("reviewItems", reviewItems);
        model.addAttribute("activeTab", "skilltests");

        return "skilltest/result";
    }

    // 6. Lịch sử bài thi của ứng viên
    @GetMapping("/candidate/skill-tests/history")
    public String candidateTestHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userService.getByEmail(principal.getUsername());
        Page<SkillTestAttempt> attemptPage = skillTestService.getAttemptsByUserPage(user.getId(), page, size);
        Map<String, Object> stats = skillTestService.getCandidateStats(user.getId());

        model.addAttribute("attempts", attemptPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", attemptPage.getTotalPages());
        model.addAttribute("totalItems", attemptPage.getTotalElements());
        model.addAttribute("stats", stats);
        model.addAttribute("activeTab", "history");

        return "skilltest/history";
    }
}
