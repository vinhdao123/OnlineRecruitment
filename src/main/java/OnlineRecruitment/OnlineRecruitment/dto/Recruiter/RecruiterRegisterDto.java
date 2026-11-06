package OnlineRecruitment.OnlineRecruitment.dto.Recruiter;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RecruiterRegisterDto {

    // --- Thông tin User (Recruiter Account) ---
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username từ 3 - 50 ký tự")
    private String username;

    @NotBlank(message = "Email cá nhân/công ty không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Họ tên người đại diện không được để trống")
    private String fullName;

    @Pattern(regexp = "^$|^0[35789][0-9]{8}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    // --- Thông tin Company ---
    @NotBlank(message = "Tên công ty không được để trống")
    private String companyName;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;

    private String companyEmail;
    private String companyPhone;
    private String companyAddress;
    private String companyWebsite;
    private String companySize;
    private String companyDescription;
}