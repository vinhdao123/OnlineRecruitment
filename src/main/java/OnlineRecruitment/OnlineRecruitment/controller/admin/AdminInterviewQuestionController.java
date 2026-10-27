package OnlineRecruitment.OnlineRecruitment.controller.admin;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import OnlineRecruitment.OnlineRecruitment.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/interview-questions")
@RequiredArgsConstructor
public class AdminInterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        Page<InterviewQuestion> questionPage = interviewQuestionService.getAllAdmin(keyword, category, page, size);

        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questionPage.getTotalPages());
        model.addAttribute("totalItems", questionPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", interviewQuestionService.getAllCategories());
        model.addAttribute("activeMenu", "interview-questions");

        return "admin/interview/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("question", new InterviewQuestion());
        model.addAttribute("categories", interviewQuestionService.getAllCategories());
        model.addAttribute("activeMenu", "interview-questions");
        return "admin/interview/form";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute("question") InterviewQuestion question,
                         RedirectAttributes ra) {
        try {
            interviewQuestionService.save(question);
            ra.addFlashAttribute("success", "Đã thêm câu hỏi phỏng vấn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/interview-questions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        InterviewQuestion question = interviewQuestionService.getById(id);
        model.addAttribute("question", question);
        model.addAttribute("categories", interviewQuestionService.getAllCategories());
        model.addAttribute("activeMenu", "interview-questions");
        return "admin/interview/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("question") InterviewQuestion form,
                       RedirectAttributes ra) {
        try {
            InterviewQuestion existing = interviewQuestionService.getById(id);
            existing.setQuestion(form.getQuestion());
            existing.setCategory(form.getCategory());
            existing.setTargetLevel(form.getTargetLevel());
            existing.setPurpose(form.getPurpose());
            existing.setTips(form.getTips());
            existing.setSampleAnswer(form.getSampleAnswer());
            existing.setPitfalls(form.getPitfalls());
            existing.setDisplayOrder(form.getDisplayOrder());
            existing.setStatus(form.getStatus());

            interviewQuestionService.save(existing);
            ra.addFlashAttribute("success", "Đã cập nhật câu hỏi phỏng vấn thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/interview-questions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            interviewQuestionService.delete(id);
            ra.addFlashAttribute("success", "Đã xóa câu hỏi phỏng vấn!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/interview-questions";
    }
}
