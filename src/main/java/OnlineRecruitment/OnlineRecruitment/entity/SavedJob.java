package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Một dòng = một tin mà ứng viên đã lưu (bảng saved_jobs)
@Entity
@Table(name = "saved_jobs")
@Getter
@Setter
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ứng viên đã lưu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Tin được lưu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    // Tự gán thời gian lưu khi thêm mới
    @PrePersist
    public void prePersist() {
        if (savedAt == null) savedAt = LocalDateTime.now();
    }
}