package OnlineRecruitment.OnlineRecruitment.controller;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import OnlineRecruitment.OnlineRecruitment.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/interview-questions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @GetMapping
    public String listQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        Page<InterviewQuestion> questionPage = interviewQuestionService.getActiveQuestions(keyword, category, page, size);
        List<String> categories = interviewQuestionService.getAllCategories();
        List<InterviewQuestion> popular = interviewQuestionService.getPopularQuestions();

        model.addAttribute("questions", questionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questionPage.getTotalPages());
        model.addAttribute("totalItems", questionPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", categories);
        model.addAttribute("popularQuestions", popular);
        model.addAttribute("activeTab", "interview");

        return "interview/list";
    }

    @GetMapping("/{id}")
    public String questionDetail(@PathVariable Long id, Model model) {
        InterviewQuestion question = interviewQuestionService.getByIdAndIncrementView(id);
        List<InterviewQuestion> popular = interviewQuestionService.getPopularQuestions();

        model.addAttribute("question", question);
        model.addAttribute("popularQuestions", popular);
        model.addAttribute("activeTab", "interview");

        return "interview/detail";
    }
}
