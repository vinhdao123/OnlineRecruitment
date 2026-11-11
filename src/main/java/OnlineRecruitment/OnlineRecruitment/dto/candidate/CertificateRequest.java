package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CertificateRequest {

    @NotBlank(message = "Tên chứng chỉ không được để trống")
    @Size(max = 255, message = "Tên chứng chỉ không quá 255 ký tự")
    private String name;

    @Size(max = 255, message = "Đơn vị cấp không quá 255 ký tự")
    private String issuer;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    @Size(max = 255, message = "Mã số chứng chỉ không quá 255 ký tự")
    private String credentialId;

    @Size(max = 1000, message = "Đường dẫn không quá 1000 ký tự")
    private String credentialUrl;

    private String description;
}