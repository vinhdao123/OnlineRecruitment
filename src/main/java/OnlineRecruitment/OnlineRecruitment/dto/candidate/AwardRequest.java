package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AwardRequest {

    @NotBlank(message = "Tên giải thưởng không được để trống")
    @Size(max = 255, message = "Tên giải thưởng không quá 255 ký tự")
    private String title;

    @Size(max = 255, message = "Đơn vị trao giải không quá 255 ký tự")
    private String issuer;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate awardDate;

    private String description;
}