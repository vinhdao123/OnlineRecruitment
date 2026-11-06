package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ActivityRequest {

    @NotBlank(message = "Tên tổ chức / hoạt động không được để trống")
    @Size(max = 255, message = "Tên tổ chức không quá 255 ký tự")
    private String organization;

    @Size(max = 255, message = "Vai trò không quá 255 ký tự")
    private String role;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Boolean isCurrent = false;

    private String description;
}