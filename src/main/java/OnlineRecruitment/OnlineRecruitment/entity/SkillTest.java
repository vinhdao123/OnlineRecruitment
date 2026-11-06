package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title; // Đề thi đánh giá kỹ năng Java

    @Column(name = "skill_name", nullable = false, length = 100)
    private String skillName; // Java, Git, HTML, MySQL, Agile, etc.

    @Column(name = "category", nullable = false, length = 100)
    private String category; // Kỹ năng công nghệ, Kỹ năng mềm, etc.

    @Column(name = "difficulty", length = 50)
    private String difficulty; // Cơ bản, Trung bình, Nâng cao

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes; // e.g. 15 phút

    @Column(name = "total_questions_per_test", nullable = false)
    private Integer totalQuestionsPerTest; // e.g. 10 câu bốc ngẫu nhiên từ pool

    @Column(name = "passing_score", nullable = false)
    private Integer passingScore; // Điểm tối thiểu đạt (ví dụ: 60/100 hoặc 6/10)

    @Column(name = "participant_count")
    private Integer participantCount; // Số lượt ứng viên đã thi

    @Column(name = "badge_color", length = 30)
    private String badgeColor; // Màu nhận diện: #2563eb, #dc2626, #16a34a, etc.

    @Column(name = "status", length = 20)
    private String status; // "ACTIVE", "DRAFT"

    @OneToMany(mappedBy = "skillTest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SkillTestQuestion> questionPool = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (durationMinutes == null) durationMinutes = 15;
        if (totalQuestionsPerTest == null) totalQuestionsPerTest = 10;
        if (passingScore == null) passingScore = 60;
        if (participantCount == null) participantCount = 0;
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
