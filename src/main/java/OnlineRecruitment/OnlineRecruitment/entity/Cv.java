package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "template_version_id", nullable = false)
    private Long templateVersionId;

    // Thêm quan hệ này để load tên Template (read-only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", insertable = false, updatable = false)
    private CvTemplateVersion templateVersion;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "professional_title", length = 255)
    private String professionalTitle;

    @Column(name = "summary", columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Column(name = "accent_color", length = 7, nullable = false)
    private String accentColor;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}