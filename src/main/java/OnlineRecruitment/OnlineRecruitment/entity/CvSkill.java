package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

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
    private Skill skill;
}