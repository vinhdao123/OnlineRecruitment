package OnlineRecruitment.OnlineRecruitment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecruiterRegisterDto {

    // =========================
    // THÔNG TIN TÀI KHOẢN
    // =========================

    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(min = 3, max = 50,
            message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Vui lòng nhập email cá nhân")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;

    // =========================
    // THÔNG TIN CÔNG TY
    // =========================

    @NotBlank(message = "Vui lòng nhập tên công ty")
    @Size(max = 255, message = "Tên công ty quá dài")
    private String companyName;

    @NotBlank(message = "Vui lòng nhập mã số thuế")
    @Size(max = 50, message = "Mã số thuế quá dài")
    private String taxCode;

    @NotBlank(message = "Vui lòng nhập email công ty")
    @Email(message = "Email công ty không hợp lệ")
    @Size(max = 255, message = "Email công ty quá dài")
    private String companyEmail;

    @NotBlank(message = "Vui lòng nhập số điện thoại công ty")
    @Size(max = 20, message = "Số điện thoại công ty quá dài")
    private String companyPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ công ty")
    @Size(max = 255, message = "Địa chỉ công ty quá dài")
    private String address;

    private String companySize;

    @Size(max = 500, message = "Website không được vượt quá 500 ký tự")
    private String website;

    private String companyDescription;

    // =========================
    // GETTER / SETTER
    // =========================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
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
}