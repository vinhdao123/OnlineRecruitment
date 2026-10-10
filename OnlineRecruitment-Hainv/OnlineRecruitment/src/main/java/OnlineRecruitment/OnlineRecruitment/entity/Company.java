package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // Nhà tuyển dụng sở hữu công ty
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    // Tên công ty
    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    // Mã số thuế
    @Column(name = "tax_code", length = 100, unique = true)
    private String taxCode;

    // Email công ty
    @Column(name = "email", length = 255)
    private String email;

    // Số điện thoại công ty
    @Column(name = "phone", length = 20)
    private String phone;

    // Địa chỉ công ty
    @Column(name = "address", length = 500)
    private String address;

    // Quy mô công ty
    @Column(name = "company_size", length = 100)
    private String companySize;

    // Website công ty
    @Column(name = "website", length = 500)
    private String website;

    // Mô tả công ty
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // Đường dẫn logo
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // Trạng thái duyệt công ty
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CompanyStatus status;

    // Lý do từ chối
    @Column(name = "rejection_reason", columnDefinition = "NVARCHAR(MAX)")
    private String rejectionReason;

    // Thời gian tạo
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời gian cập nhật
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = CompanyStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Lấy ID recruiter để tương thích với code cũ
    public Long getRecruiterId() {
        return recruiter == null ? null : recruiter.getId();
    }
}