package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReferenceRequest {

    @NotBlank(message = "Họ tên người tham chiếu không được để trống")
    @Size(max = 255, message = "Họ tên không quá 255 ký tự")
    private String fullName;

    @Size(max = 255, message = "Chức vụ không quá 255 ký tự")
    private String position;

    @Size(max = 255, message = "Công ty không quá 255 ký tự")
    private String company;

    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không quá 255 ký tự")
    private String email;

    @Size(max = 50, message = "Số điện thoại không quá 50 ký tự")
    private String phone;

    @Size(max = 255, message = "Mối quan hệ không quá 255 ký tự")
    private String relationship;
}