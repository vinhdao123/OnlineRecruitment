package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "skills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Skill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    // Kỹ năng thuộc ngành nghề nào (bảng job_categories).
    // nullable=true để giữ tương thích dữ liệu kỹ năng cũ.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}