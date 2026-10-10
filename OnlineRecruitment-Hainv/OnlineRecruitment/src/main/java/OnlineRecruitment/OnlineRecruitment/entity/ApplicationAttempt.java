package OnlineRecruitment.OnlineRecruitment.entity;

import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "application_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_attempt_candidate_job",
                columnNames = {"candidate_id", "job_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job2 job;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;
}
