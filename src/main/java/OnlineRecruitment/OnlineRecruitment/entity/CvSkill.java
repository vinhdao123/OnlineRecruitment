package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

@Entity
@Table(name = "cv_skills")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvSkill {

    @EmbeddedId
    private CvSkillId id;

    @Column(name = "proficiency", length = 50)
    private String proficiency;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // Dùng để hiển thị tên kỹ năng (Read-only)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", insertable = false, updatable = false)
    @NotFound(action = NotFoundAction.IGNORE)
    private Skill skill;
}
