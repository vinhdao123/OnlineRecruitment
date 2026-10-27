package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import OnlineRecruitment.OnlineRecruitment.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewQuestionService {

    private final InterviewQuestionRepository interviewQuestionRepository;

    @Transactional(readOnly = true)
    public Page<InterviewQuestion> getActiveQuestions(String keyword, String category, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cat = (category != null && !category.isBlank()) ? category.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        return interviewQuestionRepository.searchActive(kw, cat, pageable);
    }

    @Transactional(readOnly = true)
    public Page<InterviewQuestion> getAllAdmin(String keyword, String category, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cat = (category != null && !category.isBlank()) ? category.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        return interviewQuestionRepository.searchAllAdmin(kw, cat, pageable);
    }

    @Transactional
    public InterviewQuestion getByIdAndIncrementView(Long id) {
        InterviewQuestion q = interviewQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi phỏng vấn ID: " + id));
        q.setViewCount((q.getViewCount() == null ? 0 : q.getViewCount()) + 1);
        return interviewQuestionRepository.save(q);
    }

    @Transactional(readOnly = true)
    public InterviewQuestion getById(Long id) {
        return interviewQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi phỏng vấn ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return interviewQuestionRepository.findAllCategories();
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestion> getPopularQuestions() {
        return interviewQuestionRepository.findTop5ByStatusOrderByViewCountDesc("ACTIVE");
    }

    @Transactional
    public InterviewQuestion save(InterviewQuestion q) {
        return interviewQuestionRepository.save(q);
    }

    @Transactional
    public void delete(Long id) {
        interviewQuestionRepository.deleteById(id);
    }
}
