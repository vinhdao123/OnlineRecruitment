package OnlineRecruitment.OnlineRecruitment.dto.Recruiter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterResubmitDto {

    // --- Thông tin Người đại diện ---
    @NotBlank(message = "Họ và tên người đại diện không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^0[35789][0-9]{8}$", message = "Số điện thoại người đại diện không hợp lệ (VD: 0912345678)")
    private String phone;

    // --- Thông tin Công ty ---
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String companyName;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Size(max = 100, message = "Mã số thuế tối đa 100 ký tự")
    private String taxCode;

    @Email(message = "Email công ty không đúng định dạng")
    private String companyEmail;

    @Pattern(regexp = "^$|^[0-9+() -]{7,20}$", message = "Số điện thoại công ty không hợp lệ")
    private String companyPhone;

    @Size(max = 500, message = "Địa chỉ công ty tối đa 500 ký tự")
    private String companyAddress;

    @Size(max = 500, message = "Website tối đa 500 ký tự")
    private String companyWebsite;

    private String companySize;

    private String companyDescription;

    // --- Mật khẩu (tùy chọn: chỉ đổi nếu có nhập) ---
    private String password;
    private String confirmPassword;
}
