package OnlineRecruitment.OnlineRecruitment.dto.candidate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Thông tin một mẫu CV hiển thị trong thư viện mẫu (gallery) / bộ tùy biến. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CvGalleryTemplateVm {
    private String code;          // STANDARD, MINIMALIST, CREATIVE, MODERN, ATS_FRIENDLY
    private String name;          // Tên hiển thị
    private String filterLabel;   // Nhãn trên thanh lọc
    private Long versionId;       // id cv_template_versions (null nếu chưa có trong DB)
    private List<String> colors;  // 4 màu gợi ý
    private List<String> tags;
}