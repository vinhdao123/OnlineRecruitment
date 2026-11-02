package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cv_work_experiences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvWorkExperience {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    @Column(name = "job_title", length = 255, nullable = false)
    private String jobTitle;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}