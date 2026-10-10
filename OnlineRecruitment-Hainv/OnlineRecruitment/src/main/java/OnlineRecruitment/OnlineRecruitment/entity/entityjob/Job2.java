
package OnlineRecruitment.OnlineRecruitment.entity.entityjob;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.JobCategory;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.util.SalaryRanges;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job2 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Cột recruiter_id được quản lý bằng recruiterId.
    @Column(name = "recruiter_id", nullable = false)
    private Long recruiterId;

    // Giữ quan hệ User để tương thích với chức năng cần thông tin recruiter.
    // Quan hệ này chỉ đọc, không ghi trùng cột recruiter_id.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "recruiter_id",
            insertable = false,
            updatable = false
    )
    private User recruiter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private JobCategory category;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "requirements", columnDefinition = "NVARCHAR(MAX)")
    private String requirements;

    @Column(name = "benefits", columnDefinition = "NVARCHAR(MAX)")
    private String benefits;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(name = "experience", length = 100)
    private String experience;

    @Column(name = "level", length = 100)
    private String level;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "deadline")
    private LocalDate deadline;

    // Giữ String để tương thích với JobService hiện tại.
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "job_skills",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new LinkedHashSet<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        if (quantity == null) {
            quantity = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Các tiện ích phục vụ giao diện
    public String getCompanyName() {
        return company != null ? company.getCompanyName() : null;
    }

    public String getSalary() {
        return SalaryRanges.toLabel(salaryMin, salaryMax);
    }
}