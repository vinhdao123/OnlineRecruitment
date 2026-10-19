package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SkillRequest {

    @NotNull(message = "Vui lòng chọn kỹ năng")
    private Long skillId;

    @NotBlank(message = "Vui lòng chọn mức độ thành thạo")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED|EXPERT", message = "Mức độ không hợp lệ")
    private String proficiency;
}