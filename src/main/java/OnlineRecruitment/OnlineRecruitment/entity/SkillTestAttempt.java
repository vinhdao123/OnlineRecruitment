package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_test_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillTestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_test_id", nullable = false)
    private SkillTest skillTest;

    @Column(name = "score", nullable = false)
    private Double score; // Điểm quy đổi thang 100

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers; // Số câu đúng

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions; // Tổng số câu hỏi (ví dụ: 10)

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed; // true nếu score >= passingScore

    @Column(name = "performance_rating", length = 50)
    private String performanceRating; // "Xuất sắc", "Giỏi", "Khá", "Đạt", "Chưa đạt"

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds; // Thời gian làm bài (giây)

    @Column(name = "answers_json", columnDefinition = "NVARCHAR(MAX)")
    private String answersJson; // Chi tiết các câu hỏi đã thi, đáp án chọn, đáp án đúng để review

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        if (completedAt == null) completedAt = LocalDateTime.now();
    }
}
