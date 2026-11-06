package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cv_educations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvEducation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "institution", length = 255, nullable = false)
    private String institution;

    @Column(name = "degree", length = 255)
    private String degree;

    @Column(name = "field_of_study", length = 255)
    private String fieldOfStudy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "grade", length = 100)
    private String grade;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}