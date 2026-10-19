package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CvSummaryVm {
    private Long id;
    private String title;
    private String professionalTitle;
    private String status;
    private Boolean isDefault;
    private LocalDateTime updatedAt;
    private String templateName;
}