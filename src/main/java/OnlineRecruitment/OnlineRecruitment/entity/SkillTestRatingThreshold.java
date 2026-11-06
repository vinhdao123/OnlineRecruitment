package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bảng ngưỡng xếp loại kết quả bài thi kỹ năng.
 * Mỗi dòng định nghĩa: nếu điểm >= minScore thì xếp loại là label.
 * Các dòng được sắp xếp theo minScore giảm dần để lấy loại cao nhất trước.
 *
 * Ví dụ dữ liệu mặc định:
 *   minScore=90 → "Xuất sắc"
 *   minScore=80 → "Giỏi"
 *   minScore=65 → "Khá"
 *   minScore=50 → "Đạt"
 *   minScore=0  → "Chưa đạt"
 */
@Entity
@Table(name = "skill_test_rating_thresholds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillTestRatingThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Điểm tối thiểu (0-100) để đạt mức xếp loại này.
     * Lấy dòng đầu tiên có minScore <= score (sắp xếp giảm dần).
     */
    @Column(name = "min_score", nullable = false)
    private Integer minScore;

    /**
     * Nhãn xếp loại hiển thị, ví dụ: "Xuất sắc", "Giỏi", "Khá", "Đạt", "Chưa đạt".
     */
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    /**
     * Thứ tự hiển thị (tuỳ chọn, dùng để sort trên UI Admin).
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (displayOrder == null) displayOrder = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
