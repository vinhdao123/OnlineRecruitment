package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CreateCvRequest {
    @NotNull(message = "Vui lòng chọn mẫu CV")
    private Long templateVersionId;

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 255, message = "Tiêu đề không quá 255 ký tự")
    private String title;

    @Size(max = 255, message = "Chức danh không quá 255 ký tự")
    private String professionalTitle;

    private String summary;

    @NotBlank(message = "Màu chủ đạo không được để trống")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Màu phải có dạng #RRGGBB")
    private String accentColor = "#2563EB";

    private String avatarUrl;
}