package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cv_awards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvAward {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "issuer", length = 255)
    private String issuer;

    @Column(name = "award_date")
    private LocalDate awardDate;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // Alias methods để tương thích với template & DTO
    public String getTitle() {
        return name;
    }

    public void setTitle(String title) {
        this.name = title;
    }
}
