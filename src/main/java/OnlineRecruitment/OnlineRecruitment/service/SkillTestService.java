package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.dto.skilltest.QuestionAnswerItem;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestAttempt;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestQuestion;
import OnlineRecruitment.OnlineRecruitment.entity.SkillTestRatingThreshold;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestAttemptRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestQuestionRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestRatingThresholdRepository;
import OnlineRecruitment.OnlineRecruitment.repository.SkillTestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillTestService {

    private final SkillTestRepository skillTestRepository;
    private final SkillTestQuestionRepository skillTestQuestionRepository;
    private final SkillTestAttemptRepository skillTestAttemptRepository;
    private final SkillTestRatingThresholdRepository ratingThresholdRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Page<SkillTest> getActiveTests(String keyword, String category, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cat = (category != null && !category.isBlank()) ? category.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        return skillTestRepository.searchActive(kw, cat, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SkillTest> getAllAdmin(String keyword, String category, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        String cat = (category != null && !category.isBlank()) ? category.trim() : null;
        Pageable pageable = PageRequest.of(page, size);
        return skillTestRepository.searchAllAdmin(kw, cat, pageable);
    }

    @Transactional(readOnly = true)
    public SkillTest getById(Long id) {
        return skillTestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài đánh giá kỹ năng ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return skillTestRepository.findAllCategories();
    }

    @Transactional(readOnly = true)
    public List<SkillTest> getPopularTests() {
        return skillTestRepository.findTop6ByStatusOrderByParticipantCountDesc("ACTIVE");
    }

    /**
     * Bốc ngẫu nhiên N câu hỏi từ Question Pool của bài thi
     */
    @Transactional(readOnly = true)
    public List<SkillTestQuestion> getRandomQuestionsForTest(Long testId) {
        SkillTest test = getById(testId);
        List<SkillTestQuestion> pool = skillTestQuestionRepository.findActivePoolByTestId(testId);
        if (pool.isEmpty()) {
            throw new IllegalStateException("Bài thi này hiện chưa có câu hỏi nào trong ngân hàng đề thi!");
        }

        // Shuffle pool
        List<SkillTestQuestion> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);

        int count = Math.min(test.getTotalQuestionsPerTest() != null ? test.getTotalQuestionsPerTest() : 10, shuffled.size());
        return shuffled.subList(0, count);
    }

    /**
     * Nộp bài thi, chấm điểm tự động và lưu lịch sử
     */
    @Transactional
    public SkillTestAttempt submitTest(Long testId, User user, Map<String, String> submittedAnswers, int timeSpentSeconds) {
        SkillTest test = getById(testId);

        // Lấy danh sách câu hỏi đã nộp
        List<Long> questionIds = new ArrayList<>();
        for (String key : submittedAnswers.keySet()) {
            if (key.startsWith("q_")) {
                try {
                    questionIds.add(Long.parseLong(key.substring(2)));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (questionIds.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy câu hỏi nào trong bài nộp!");
        }

        List<SkillTestQuestion> questions = skillTestQuestionRepository.findAllById(questionIds);
        int correctCount = 0;
        List<QuestionAnswerItem> reviewItems = new ArrayList<>();

        for (SkillTestQuestion q : questions) {
            String selected = submittedAnswers.get("q_" + q.getId());
            boolean isCorrect = selected != null && selected.trim().equalsIgnoreCase(q.getCorrectOption().trim());
            if (isCorrect) {
                correctCount++;
            }

            reviewItems.add(QuestionAnswerItem.builder()
                    .questionId(q.getId())
                    .questionText(q.getQuestionText())
                    .optionA(q.getOptionA())
                    .optionB(q.getOptionB())
                    .optionC(q.getOptionC())
                    .optionD(q.getOptionD())
                    .selectedOption(selected != null ? selected.trim().toUpperCase() : null)
                    .correctOption(q.getCorrectOption().trim().toUpperCase())
                    .isCorrect(isCorrect)
                    .explanation(q.getExplanation())
                    .build());
        }

        int totalQuestions = questions.size();
        double score = totalQuestions > 0 ? Math.round(((double) correctCount / totalQuestions) * 100.0) : 0.0;
        boolean isPassed = score >= (test.getPassingScore() != null ? test.getPassingScore() : 60);

        // Tra cứu xếp loại từ bảng ngưỡng trong database (không hardcode)
        String rating = resolveRatingFromDB(score);

        String json = "[]";
        try {
            json = objectMapper.writeValueAsString(reviewItems);
        } catch (Exception e) {
            log.error("Failed to serialize review items: {}", e.getMessage());
        }

        SkillTestAttempt attempt = new SkillTestAttempt();
        attempt.setUser(user);
        attempt.setSkillTest(test);
        attempt.setScore(score);
        attempt.setCorrectAnswers(correctCount);
        attempt.setTotalQuestions(totalQuestions);
        attempt.setIsPassed(isPassed);
        attempt.setPerformanceRating(rating);
        attempt.setTimeSpentSeconds(timeSpentSeconds);
        attempt.setAnswersJson(json);
        attempt.setCompletedAt(LocalDateTime.now());

        SkillTestAttempt saved = skillTestAttemptRepository.save(attempt);

        // Tăng số lượt thi của bài test
        test.setParticipantCount((test.getParticipantCount() == null ? 0 : test.getParticipantCount()) + 1);
        skillTestRepository.save(test);

        return saved;
    }

    @Transactional(readOnly = true)
    public SkillTestAttempt getAttemptById(Long attemptId) {
        return skillTestAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kết quả bài thi ID: " + attemptId));
    }

    public List<QuestionAnswerItem> parseReviewItems(String answersJson) {
        if (answersJson == null || answersJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(answersJson, new TypeReference<List<QuestionAnswerItem>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize review items: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<SkillTestAttempt> getAttemptsByUser(Long userId) {
        return skillTestAttemptRepository.findByUserIdOrderByCompletedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Page<SkillTestAttempt> getAttemptsByUserPage(Long userId, int page, int size) {
        return skillTestAttemptRepository.findByUserId(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCandidateStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        long totalTests = skillTestAttemptRepository.countByUserId(userId);
        long passedTests = skillTestAttemptRepository.countPassedByUserId(userId);
        Double avg = skillTestAttemptRepository.getAverageScoreByUserId(userId);
        stats.put("totalTests", totalTests);
        stats.put("passedTests", passedTests);
        stats.put("avgScore", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        return stats;
    }

    // --- Admin Operations ---

    @Transactional
    public SkillTest saveTest(SkillTest test) {
        return skillTestRepository.save(test);
    }

    @Transactional
    public void deleteTest(Long testId) {
        skillTestRepository.deleteById(testId);
    }

    @Transactional(readOnly = true)
    public List<SkillTestQuestion> getQuestionsByTestId(Long testId) {
        return skillTestQuestionRepository.findBySkillTestId(testId);
    }

    @Transactional(readOnly = true)
    public SkillTestQuestion getQuestionById(Long questionId) {
        return skillTestQuestionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi ID: " + questionId));
    }

    @Transactional
    public SkillTestQuestion saveQuestion(SkillTestQuestion question) {
        return skillTestQuestionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        skillTestQuestionRepository.deleteById(questionId);
    }

    @Transactional(readOnly = true)
    public long countQuestionsInPool(Long testId) {
        return skillTestQuestionRepository.countBySkillTestId(testId);
    }

    /**
     * Tra cứu nhãn xếp loại từ bảng ngưỡng trong database.
     * Lấy dòng đầu tiên có minScore <= score (đã sort giảm dần).
     * Nếu DB chưa có dữ liệu, fallback về logic mặc định để tránh lỗi runtime.
     */
    @Transactional(readOnly = true)
    public String resolveRatingFromDB(double score) {
        List<SkillTestRatingThreshold> matches = ratingThresholdRepository.findMatchingThresholds(score);
        if (!matches.isEmpty()) {
            return matches.get(0).getLabel();
        }
        // Fallback mặc định nếu bảng ngưỡng chưa được seed
        log.warn("skill_test_rating_thresholds is empty; using built-in fallback for score={}", score);
        if (score >= 90) return "Xuất sắc";
        else if (score >= 80) return "Giỏi";
        else if (score >= 65) return "Khá";
        else if (score >= 50) return "Đạt";
        else return "Chưa đạt";
    }

    /**
     * Lấy tất cả ngưỡng xếp loại (dùng cho trang Admin quản lý).
     */
    @Transactional(readOnly = true)
    public List<SkillTestRatingThreshold> getAllRatingThresholds() {
        return ratingThresholdRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Lưu (tạo mới hoặc cập nhật) ngưỡng xếp loại.
     */
    @Transactional
    public SkillTestRatingThreshold saveRatingThreshold(SkillTestRatingThreshold threshold) {
        return ratingThresholdRepository.save(threshold);
    }

    /**
     * Xóa ngưỡng xếp loại theo ID.
     */
    @Transactional
    public void deleteRatingThreshold(Long thresholdId) {
        ratingThresholdRepository.deleteById(thresholdId);
    }
}
