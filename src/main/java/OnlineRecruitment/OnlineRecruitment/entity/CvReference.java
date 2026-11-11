package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cv_references")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CvReference {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cv_id", nullable = false)
    private Long cvId;

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "position", length = 255)
    private String position;

    @Column(name = "organization", length = 255)
    private String organization;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "relationship", length = 255)
    private String relationship;

    // Alias methods để tương thích với template & DTO
    public String getCompany() {
        return organization;
    }

    public void setCompany(String company) {
        this.organization = company;
    }
}
