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

    // Giữ nullable để các kỹ năng cũ chưa phân loại tiếp tục dùng được trong CV.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private JobCategory category;

    public Skill(Long id, String name, String status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
