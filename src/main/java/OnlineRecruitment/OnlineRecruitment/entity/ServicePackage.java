package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ServicePackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false)
    private UserRole targetRole;

    @Column(name = "is_active")
    private Boolean isActive = true; // Mặc định là true khi tạo mới

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Tự động gán ngày tạo trước khi lưu vào DB
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}