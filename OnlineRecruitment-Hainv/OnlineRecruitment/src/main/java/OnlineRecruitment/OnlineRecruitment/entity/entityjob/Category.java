package OnlineRecruitment.OnlineRecruitment.entity.entityjob;

import jakarta.persistence.*;

@Entity
@Table(name = "job_categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String status;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}