package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cv_template_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvTemplateVersion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "layout_definition", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String layoutDefinition;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", insertable = false, updatable = false)
    private CvTemplate template;
}