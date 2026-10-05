package OnlineRecruitment.OnlineRecruitment.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecruiterRegisterDto {

    // 1. THÔNG TIN NGƯỜI ĐẠI DIỆN
    @NotBlank(message = "Tên người dùng không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$", message = "Tên người dùng chỉ gồm chữ cái, số, gạch dưới và từ 3-50 ký tự")
    private String username;

    @NotBlank(message = "Email cá nhân không được để trống")
    @Email(message = "Email cá nhân không đúng định dạng")
    private String personalEmail;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận lại mật khẩu")
    private String confirmPassword;

    // 2. THÔNG TIN CÔNG TY
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 200, message = "Tên công ty tối đa 200 ký tự")
    private String companyName;

    @NotBlank(message = "Mã số thuế không được để trống")
    @Pattern(regexp = "^[a-zA-Z0-9-]{10,20}$", message = "Mã số thuế không đúng định dạng (10-20 ký tự chữ và số)")
    private String taxCode;

    @Email(message = "Email công ty không đúng định dạng")
    private String companyEmail;

    private String companyPhone;

    private String companySize;

    private String companyWebsite;

    private String companyAddress;

    private String companyDescription;
}
