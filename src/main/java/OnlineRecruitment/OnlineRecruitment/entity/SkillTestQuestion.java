package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_test_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillTestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_test_id", nullable = false)
    private SkillTest skillTest;

    @Column(name = "question_text", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionText;

    @Column(name = "option_a", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String optionA;

    @Column(name = "option_b", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String optionB;

    @Column(name = "option_c", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String optionC;

    @Column(name = "option_d", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String optionD;

    @Column(name = "correct_option", nullable = false, length = 10)
    private String correctOption; // "A", "B", "C", "D"

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation; // Lời giải thích chi tiết đáp án

    @Column(name = "status", length = 20)
    private String status; // "ACTIVE"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null) status = "ACTIVE";
    }
}
