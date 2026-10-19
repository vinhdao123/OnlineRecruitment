package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvTemplateOptionVm {
    private Long id; // Chính là templateVersionId
    private String templateName;
    private Integer versionNumber;
}