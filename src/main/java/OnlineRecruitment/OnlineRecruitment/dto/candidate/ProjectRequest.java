package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "Tên dự án không được để trống")
    @Size(max = 255, message = "Tên dự án không quá 255 ký tự")
    private String name;

    @Size(max = 255, message = "Vai trò không quá 255 ký tự")
    private String role;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Size(max = 1000, message = "Đường dẫn không quá 1000 ký tự")
    private String projectUrl;

    private String description;

    private String technologies;
}