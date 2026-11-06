package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Data
public class CandidateProfileDto {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên tối đa 255 ký tự")
    private String fullName;

    @Pattern(regexp = "^$|^0[35789][0-9]{8}$", message = "Số điện thoại không đúng định dạng (VD: 0912345678)")
    private String phone;

    private String address;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dob;

    private String gender;

    @Size(max = 255, message = "Vị trí mong muốn tối đa 255 ký tự")
    private String desiredPosition;

    private String aboutMe;

    @Size(max = 500, message = "Link LinkedIn không vượt quá 500 ký tự")
    private String linkedinUrl;

    @Size(max = 500, message = "Link GitHub không vượt quá 500 ký tự")
    private String githubUrl;

    @Size(max = 500, message = "Link Portfolio không vượt quá 500 ký tự")
    private String portfolioUrl;

    private MultipartFile avatarFile;
}
