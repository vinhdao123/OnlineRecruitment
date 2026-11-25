package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class CandidateRegisterDto {

    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username từ 3 - 50 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(?:\\+84|0)[1-9][0-9]{8,9}$", message = "Số điện thoại phải từ 10 - 11 chữ số hợp lệ theo quy chuẩn hiện tại (VD: 0912345678, 02412345678)")
    private String phone;
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
//    private LocalDate dob;
//    @NotNull(message = "Ngày sinh không được để trống")
//    @Past(message = "Ngày sinh không được phép ở trong tương lai")
//    private String gender;

    // Ngày sinh
    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh không được phép ở trong tương lai")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;
    // Giới tính
    @NotBlank(message = "Vui lòng chọn giới tính")
    private String gender;

    private String address;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;
}