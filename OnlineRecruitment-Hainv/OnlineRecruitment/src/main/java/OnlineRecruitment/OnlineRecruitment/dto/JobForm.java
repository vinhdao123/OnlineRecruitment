package OnlineRecruitment.OnlineRecruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JobForm {

    @NotBlank(message = "Vui lòng nhập tiêu đề công việc")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @Size(max = 100)
    private String level = "Intern";

    @NotNull(message = "Vui lòng chọn danh mục nghề nghiệp")
    private Long categoryId;

    @NotBlank(message = "Vui lòng chọn tỉnh/thành phố")
    @Size(max = 255)
    private String location;

    @NotNull(message = "Vui lòng nhập số lượng tuyển")
    @Min(value = 1, message = "Số lượng tuyển tối thiểu là 1")
    private Integer quantity;

    @NotNull(message = "Vui lòng chọn hạn nộp hồ sơ")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline;

    private List<Long> skillIds = new ArrayList<>();

    @NotBlank(message = "Vui lòng chọn mức lương")
    private String salary = "Dưới 10 triệu";

    @Size(max = 100)
    private String experience = "Không yêu cầu";

    @NotBlank(message = "Vui lòng nhập mô tả công việc")
    private String description;

    @NotBlank(message = "Vui lòng nhập yêu cầu ứng viên")
    private String requirements;

    private String benefits;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public List<Long> getSkillIds() {
        return skillIds;
    }

    public void setSkillIds(List<Long> skillIds) {
        this.skillIds = skillIds;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }
}