package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "cv_certificates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvCertificate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "issuing_org", length = 255)
    private String issuingOrg;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "credential_id", length = 255)
    private String credentialId;

    @Column(name = "credential_url", length = 1000)
    private String credentialUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // Alias methods để tương thích với template & DTO
    public String getIssuer() {
        return issuingOrg;
    }

    public void setIssuer(String issuer) {
        this.issuingOrg = issuer;
    }
}
