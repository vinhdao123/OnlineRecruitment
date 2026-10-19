package OnlineRecruitment.OnlineRecruitment.dto.Recruiter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class RecruiterProfileDto {

    // Thông tin người đại diện (User)
    @NotBlank(message = "Họ và tên người đại diện không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^0[35789][0-9]{8}$", message = "Số điện thoại không đúng định dạng (VD: 0912345678)")
    private String phone;

    private String location;

    private MultipartFile avatarFile;

    // Thông tin Công ty (Company)
    @NotBlank(message = "Tên công ty không được để trống")
    @Size(max = 255, message = "Tên công ty tối đa 255 ký tự")
    private String companyName;

    private String taxCode;

    private String companyEmail;

    @Pattern(regexp = "^$|^[0-9+() -]{7,20}$", message = "Số điện thoại công ty không hợp lệ")
    private String companyPhone;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String companyAddress;

    @Size(max = 500, message = "Website tối đa 500 ký tự")
    private String companyWebsite;

    private String companySize;

    private String companyDescription;

    private MultipartFile logoFile;
}
