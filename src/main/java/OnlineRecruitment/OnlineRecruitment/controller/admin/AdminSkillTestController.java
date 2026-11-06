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

    @PostMapping("/create")
    public String create(@ModelAttribute("test") SkillTest test, RedirectAttributes ra) {
        try {
            skillTestService.saveTest(test);
            ra.addFlashAttribute("success", "Đã tạo mới bộ đề thi kỹ năng thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests";
    }

    // 3. Chỉnh sửa thông tin đề thi
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        SkillTest test = skillTestService.getById(id);
        model.addAttribute("test", test);
        model.addAttribute("activeMenu", "skill-tests");
        return "admin/skilltest/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("test") SkillTest form,
                       RedirectAttributes ra) {
        try {
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
            SkillTest test = skillTestService.getById(id);
            question.setSkillTest(test);
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
            SkillTestQuestion q = skillTestService.getQuestionById(questionId);
            q.setQuestionText(questionText);
            q.setOptionA(optionA);
            q.setOptionB(optionB);
            q.setOptionC(optionC);
            q.setOptionD(optionD);
            q.setCorrectOption(correctOption);
            q.setExplanation(explanation);

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
            skillTestService.deleteQuestion(questionId);
            ra.addFlashAttribute("success", "Đã xóa câu hỏi khỏi ngân hàng!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/skill-tests/" + testId + "/questions";
    }
}
