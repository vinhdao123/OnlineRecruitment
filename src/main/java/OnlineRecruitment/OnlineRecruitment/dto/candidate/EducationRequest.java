package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EducationRequest {

    @NotBlank(message = "Tên trường/Tổ chức không được để trống")
    @Size(max = 255, message = "Tên trường không quá 255 ký tự")
    private String institution;

    @Size(max = 255)
    private String degree;

    @Size(max = 255)
    private String fieldOfStudy;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Trường mới: Xếp loại (Dropdown)
    private String academicRank; // Nhận giá trị: "Trung bình", "Khá", "Giỏi"

    // Trường mới: GPA (0 - 4.0)
    @DecimalMin(value = "0.0", message = "GPA không được nhỏ hơn 0")
    @DecimalMax(value = "4.0", message = "GPA không được lớn hơn 4.0")
    private Double gpa;

    private String description;
}