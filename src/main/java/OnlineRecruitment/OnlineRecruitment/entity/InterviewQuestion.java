package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question", nullable = false, length = 500)
    private String question;

    @Column(name = "category", nullable = false, length = 100)
    private String category; // e.g. "Phỏng vấn chung", "Tình huống & Hành vi", "Kỹ thuật / IT", "Đàm phán lương"

    @Column(name = "target_level", length = 50)
    private String targetLevel; // e.g. "Mọi cấp bậc", "Fresher / Thực tập sinh", "Junior / Middle", "Senior / Lead"

    @Column(name = "purpose", columnDefinition = "NVARCHAR(MAX)")
    private String purpose; // Ý đồ của nhà tuyển dụng

    @Column(name = "tips", columnDefinition = "NVARCHAR(MAX)")
    private String tips; // Hướng dẫn & Dàn ý trả lời

    @Column(name = "sample_answer", columnDefinition = "NVARCHAR(MAX)")
    private String sampleAnswer; // Câu trả lời mẫu ấn tượng

    @Column(name = "pitfalls", columnDefinition = "NVARCHAR(MAX)")
    private String pitfalls; // Những điều nên tránh

    @Column(name = "view_count")
    private Integer viewCount;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "status", length = 20)
    private String status; // "ACTIVE", "HIDDEN"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (viewCount == null) viewCount = 0;
        if (displayOrder == null) displayOrder = 0;
        if (status == null) status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
