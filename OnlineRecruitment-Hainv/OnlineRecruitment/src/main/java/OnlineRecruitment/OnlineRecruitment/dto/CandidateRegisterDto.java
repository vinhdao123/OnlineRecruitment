package OnlineRecruitment.OnlineRecruitment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CandidateRegisterDto {

    // =========================
    // EMAIL
    // =========================

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    // =========================
    // HỌ VÀ TÊN
    // =========================

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
    private String fullName;

    // =========================
    // SỐ ĐIỆN THOẠI
    // =========================

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    // =========================
    // MẬT KHẨU
    // =========================

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, max = 72,
            message = "Mật khẩu phải từ 6 đến 72 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    // =========================
    // NGÀY SINH
    // =========================

    @NotNull(message = "Vui lòng chọn ngày sinh")
    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    // =========================
    // GIỚI TÍNH
    // =========================

    @NotBlank(message = "Vui lòng chọn giới tính")
    @Pattern(
            regexp = "Male|Female|Other",
            message = "Giới tính không hợp lệ"
    )
    private String gender;

    // =========================
    // ĐỊA CHỈ
    // =========================

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    // =========================
    // GETTER / SETTER
    // =========================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}