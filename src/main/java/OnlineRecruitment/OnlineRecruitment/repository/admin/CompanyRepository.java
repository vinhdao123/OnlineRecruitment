package OnlineRecruitment.OnlineRecruitment.repository.admin;

import OnlineRecruitment.OnlineRecruitment.entity.Company;
import OnlineRecruitment.OnlineRecruitment.entity.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByRecruiterId(Long recruiterId);

    long countByStatus(CompanyStatus status);

    @Query("""
        SELECT c FROM Company c
        LEFT JOIN FETCH c.recruiter
        WHERE c.id = :id
    """)
    Optional<Company> findByIdWithRecruiter(@Param("id") Long id);

    @Query(value = """
        SELECT c FROM Company c
        LEFT JOIN FETCH c.recruiter
        WHERE (:keyword IS NULL OR
               LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(c.taxCode)     LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(c.email)       LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR c.status = :status)
    """,
            countQuery = """
        SELECT COUNT(c) FROM Company c
        WHERE (:keyword IS NULL OR
               LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(c.taxCode)     LIKE LOWER(CONCAT('%', :keyword, '%')) OR
               LOWER(c.email)       LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR c.status = :status)
    """)
    Page<Company> search(@Param("keyword") String keyword,
                         @Param("status") CompanyStatus status,
                         Pageable pageable);

    @Query("SELECT c FROM Company c WHERE c.id = :id AND c.status = 'APPROVED'")
    Optional<Company> findApprovedById(@Param("id") Long id);

    /** Lấy N công ty mới nhất — dùng cho dashboard. */
    @Query("""
        SELECT c FROM Company c
        LEFT JOIN FETCH c.recruiter
        ORDER BY c.createdAt DESC
    """)
    List<Company> findRecent(Pageable pageable);
}