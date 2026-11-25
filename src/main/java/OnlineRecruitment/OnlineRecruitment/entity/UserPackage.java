package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_packages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // CHÚ Ý: Phải là package_id theo đúng DB của bạn
    @ManyToOne
    @JoinColumn(name = "package_id")
    private ServicePackage servicePackage;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;   // ACTIVE / EXPIRED / CANCELLED

    @Column(name = "used_jobs")
    private Integer usedJobs = 0;

    @Column(name = "snapshot_max_jobs")
    private Integer snapshotMaxJobs;

    @Column(name = "allow_pin_job")
    private Boolean allowPinJob = false;
}