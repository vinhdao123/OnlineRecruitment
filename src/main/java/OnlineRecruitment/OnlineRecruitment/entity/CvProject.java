package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cv_projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvProject {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "role", length = 255)
    private String role;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "project_url", length = 1000)
    private String projectUrl;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "technologies", columnDefinition = "NVARCHAR(MAX)")
    private String technologies;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}