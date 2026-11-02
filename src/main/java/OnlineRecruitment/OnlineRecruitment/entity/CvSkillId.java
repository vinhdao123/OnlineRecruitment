package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class CvSkillId implements Serializable {
    @Column(name = "cv_id")
    private Long cvId;

    @Column(name = "skill_id")
    private Long skillId;
}