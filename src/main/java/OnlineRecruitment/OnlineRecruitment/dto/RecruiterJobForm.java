package OnlineRecruitment.OnlineRecruitment.dto;

import OnlineRecruitment.OnlineRecruitment.entity.EmploymentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

// Dữ liệu form tạo/sửa tin tuyển dụng
// (@Getter/@Setter của Lombok tự sinh getLogoUrl()/setLogoUrl()...)
@Getter
@Setter
@NoArgsConstructor
public class RecruiterJobForm {

    private Long jobId;

    private Long categoryId;

    private String title;

    private String description;

    private String requirements;

    private String benefits;

    private String location;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String salaryCurrency = "VND";

    private String experience;

    private String level;

    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    private Integer quantity = 1;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    // URL logo công ty (không bắt buộc, lưu vào companies.logo_url)
    private String logoUrl;
}