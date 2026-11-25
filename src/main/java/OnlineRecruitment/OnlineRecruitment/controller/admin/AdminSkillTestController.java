package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import OnlineRecruitment.OnlineRecruitment.service.SkillTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/skill-tests")
@RequiredArgsConstructor
public class AdminSkillTestController {

    private final SkillTestService skillTestService;

    // 1. Danh sách các bài thi kỹ năng
    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        Page<SkillTest> testPage = skillTestService.getAllAdmin(keyword, category, page, size);

        model.addAttribute("tests", testPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", testPage.getTotalPages());
        model.addAttribute("totalItems", testPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", skillTestService.getAllCategories());
        model.addAttribute("activeMenu", "skill-tests");

        return "admin/skilltest/list";
    }

    // 2. Tạo mới đề thi kỹ năng
    @GetMapping("/create")
    public String createForm(Model model) {
        SkillTest test = new SkillTest();
        test.setDurationMinutes(15);
        test.setTotalQuestionsPerTest(10);
        test.setPassingScore(60);
        test.setBadgeColor("#2563eb");
        test.setCategory("Kỹ năng công nghệ");
        test.setDifficulty("Trung bình");

        model.addAttribute("test", test);
        model.addAttribute("activeMenu", "skill-tests");
        return "admin/skilltest/form";
    }

    private void validateSkillTest(SkillTest test) {
        if (test.getTitle() == null || test.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tiêu đề bài thi không được để trống!");
        }
        if (test.getSkillName() == null || test.getSkillName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên kỹ năng không được để trống!");
        }
        if (test.getDurationMinutes() == null || test.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Thời gian làm bài phải lớn hơn 0 phút!");
        }
        if (test.getTotalQuestionsPerTest() == null || test.getTotalQuestionsPerTest() <= 0) {
            throw new IllegalArgumentException("Số câu hỏi bốc ngẫu nhiên phải lớn hơn hoặc bằng 1!");
        }
        if (test.getPassingScore() == null || test.getPassingScore() < 1 || test.getPassingScore() > 100) {
            throw new IllegalArgumentException("Điểm đạt tối thiểu phải nằm trong khoảng từ 1 đến 100!");
        }
    }

    private void validateQuestion(String questionText, String optionA, String optionB, String optionC, String optionD, String correctOption) {
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được để trống!");
        }
        if (optionA == null || optionA.trim().isEmpty() ||
            optionB == null || optionB.trim().isEmpty() ||
            optionC == null || optionC.trim().isEmpty() ||
            optionD == null || optionD.trim().isEmpty()) {
            throw new IllegalArgumentException("Tất cả 4 phương án A, B, C, D đều không được để trống!");
        }
        String clean = correctOption != null ? correctOption.trim().toUpperCase() : "";
        if (!List.of("A", "B", "C", "D").contains(clean)) {
            throw new IllegalArgumentException("Đáp án đúng phải là một trong các phương án: A, B, C hoặc D!");
        }
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("test") SkillTest test, RedirectAttributes ra) {
        try {
            validateSkillTest(test);
            SkillTest saved = skillTestService.saveTest(test);
            ra.addFlashAttribute("success", "Đã tạo mới bộ đề thi thành công! Vui lòng thêm các câu hỏi vào ngân hàng đề thi bên dưới (yêu cầu tối thiểu " + saved.getTotalQuestionsPerTest() + " câu).");
            return "redirect:/admin/skill-tests/" + saved.getId() + "/questions";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/skill-tests";
        }
    }

    // 3. Chỉnh sửa thông tin đề thi
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SkillTest test = skillTestService.getById(id);
        long poolCount = skillTestService.countActiveQuestionsInPool(id);
        model.addAttribute("test", test);
        model.addAttribute("poolCount", poolCount);
        model.addAttribute("activeMenu", "skill-tests");
        return "admin/skilltest/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("test") SkillTest form,
                       RedirectAttributes ra) {
        try {
            validateSkillTest(form);
            SkillTest existing = skillTestService.getById(id);
            existing.setTitle(form.getTitle());
            existing.setSkillName(form.getSkillName());
            existing.setCategory(form.getCategory());
            existing.setDifficulty(form.getDifficulty());
            existing.setDescription(form.getDescription());
            existing.setDurationMinutes(form.getDurationMinutes());
            existing.setTotalQuestionsPerTest(form.getTotalQuestionsPerTest());
            existing.setPassingScore(form.getPassingScore());
            existing.setBadgeColor(form.getBadgeColor());
            existing.setStatus(form.getStatus());

            skillTestService.saveTest(existing);
            ra.addFlashAttribute("success", "Đã cập nhật bộ đề thi thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            skillTestService.deleteTest(id);
            ra.addFlashAttribute("success", "Đã xóa bộ đề thi!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests";
    }

    // ==========================================
    // QUẢN LÝ NGÂN HÀNG CÂU HỎI (QUESTION POOL)
    // ==========================================

    @GetMapping("/{id}/questions")
    public String questionPool(@PathVariable Long id, Model model) {
        SkillTest test = skillTestService.getById(id);
        List<SkillTestQuestion> questions = skillTestService.getQuestionsByTestId(id);

        SkillTestQuestion newQuestion = new SkillTestQuestion();
        newQuestion.setSkillTest(test);
        newQuestion.setCorrectOption("A");

        model.addAttribute("test", test);
        model.addAttribute("questions", questions);
        model.addAttribute("newQuestion", newQuestion);
        model.addAttribute("activeMenu", "skill-tests");

        return "admin/skilltest/questions";
    }

    @PostMapping("/{id}/questions/create")
    public String addQuestion(@PathVariable Long id,
                              @ModelAttribute("newQuestion") SkillTestQuestion question,
                              RedirectAttributes ra) {
        try {
            validateQuestion(question.getQuestionText(), question.getOptionA(), question.getOptionB(),
                    question.getOptionC(), question.getOptionD(), question.getCorrectOption());
            SkillTest test = skillTestService.getById(id);
            question.setSkillTest(test);
            question.setCorrectOption(question.getCorrectOption().trim().toUpperCase());
            if (question.getStatus() == null || question.getStatus().isBlank()) {
                question.setStatus("ACTIVE");
            }
            if (question.getCreatedAt() == null) {
                question.setCreatedAt(java.time.LocalDateTime.now());
            }
            skillTestService.saveQuestion(question);
            ra.addFlashAttribute("success", "Đã thêm câu hỏi vào ngân hàng đề thi thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi thêm câu hỏi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests/" + id + "/questions";
    }

    @PostMapping("/{testId}/questions/{questionId}/edit")
    public String editQuestion(@PathVariable Long testId,
                               @PathVariable Long questionId,
                               @RequestParam String questionText,
                               @RequestParam String optionA,
                               @RequestParam String optionB,
                               @RequestParam String optionC,
                               @RequestParam String optionD,
                               @RequestParam String correctOption,
                               @RequestParam(required = false) String explanation,
                               RedirectAttributes ra) {
        try {
            validateQuestion(questionText, optionA, optionB, optionC, optionD, correctOption);
            SkillTestQuestion q = skillTestService.getQuestionById(questionId);
            if (!q.getSkillTest().getId().equals(testId)) {
                throw new IllegalArgumentException("Câu hỏi không thuộc đề thi này!");
            }
            q.setQuestionText(questionText);
            q.setOptionA(optionA);
            q.setOptionB(optionB);
            q.setOptionC(optionC);
            q.setOptionD(optionD);
            q.setCorrectOption(correctOption.trim().toUpperCase());
            q.setExplanation(explanation);
            if (q.getStatus() == null || q.getStatus().isBlank()) {
                q.setStatus("ACTIVE");
            }

            skillTestService.saveQuestion(q);
            ra.addFlashAttribute("success", "Đã cập nhật câu hỏi thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi cập nhật câu hỏi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests/" + testId + "/questions";
    }

    @PostMapping("/{testId}/questions/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long testId,
                                 @PathVariable Long questionId,
                                 RedirectAttributes ra) {
        try {
            SkillTestQuestion q = skillTestService.getQuestionById(questionId);
            if (!q.getSkillTest().getId().equals(testId)) {
                throw new IllegalArgumentException("Câu hỏi không thuộc đề thi này!");
            }
            skillTestService.deleteQuestion(questionId);
            ra.addFlashAttribute("success", "Đã xóa câu hỏi khỏi ngân hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests/" + testId + "/questions";
    }
}
