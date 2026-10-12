package OnlineRecruitment.OnlineRecruitment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyProfileUpdateDto(

        @NotBlank(message = "Tên công ty không được để trống")
        @Size(max = 255)
        String companyName,

        @NotBlank(message = "Mã số thuế không được để trống")
        @Size(max = 50)
        String taxCode,

        @NotBlank(message = "Email công ty không được để trống")
        @Email(message = "Email công ty không hợp lệ")
        @Size(max = 255)
        String email,

        @Size(max = 20)
        String phone,

        @NotBlank(message = "Địa chỉ công ty không được để trống")
        @Size(max = 500)
        String address,

        @Size(max = 100)
        String companySize,

        @Size(max = 255)
        String website,

        @Size(max = 5000)
        String description,

        @Size(max = 500)
        String logoUrl
) {
}