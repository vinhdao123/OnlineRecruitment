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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    private ServicePackage servicePackage;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status; // "ACTIVE", "EXPIRED"
}