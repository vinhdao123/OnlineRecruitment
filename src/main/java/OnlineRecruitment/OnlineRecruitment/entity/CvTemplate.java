package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cv_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 100, nullable = false, unique = true)
    private String code;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}