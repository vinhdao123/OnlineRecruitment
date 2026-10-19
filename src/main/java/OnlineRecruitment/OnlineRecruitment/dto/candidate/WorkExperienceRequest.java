package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WorkExperienceRequest {

    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty không quá 255 ký tự")
    private String companyName;

    @NotBlank(message = "Chức danh không được để trống")
    @Size(max = 255, message = "Chức danh không quá 255 ký tự")
    private String jobTitle;

    @Size(max = 255)
    private String location;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Boolean isCurrent = false;

    private String description;
}