package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

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

    // ===== KIỂM TRA MÃ SỐ THUẾ =====

    boolean existsByTaxCode(String taxCode);

    // ===== TÌM KIẾM CÔNG TY, CÓ PHÂN TRANG =====

    @Query("""
            SELECT c
            FROM Company c
            WHERE (
                :keyword IS NULL
                OR LOWER(c.companyName)
                   LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :status IS NULL
                OR c.status = :status
            )
            """)
    Page<Company> search(
            @Param("keyword") String keyword,
            @Param("status") CompanyStatus status,
            Pageable pageable
    );

    // ===== TÌM CÔNG TY KÈM THÔNG TIN RECRUITER =====

    @Query("""
            SELECT c
            FROM Company c
            LEFT JOIN FETCH c.recruiter
            WHERE c.id = :id
            """)
    Optional<Company> findByIdWithRecruiter(
            @Param("id") Long id
    );

    // ===== TÌM CÔNG TY THEO RECRUITER =====

    Optional<Company> findFirstByRecruiter_IdOrderByCreatedAtDesc(
            Long recruiterId
    );

    default Optional<Company> findByRecruiterId(Long recruiterId) {
        return findFirstByRecruiter_IdOrderByCreatedAtDesc(recruiterId);
    }

    default Optional<Company> findFirstByRecruiterId(Long recruiterId) {
        return findFirstByRecruiter_IdOrderByCreatedAtDesc(recruiterId);
    }

    // ===== ĐẾM CÔNG TY THEO TRẠNG THÁI =====

    long countByStatus(CompanyStatus status);

    // ===== DANH SÁCH CÔNG TY MỚI NHẤT =====

    @Query("""
            SELECT c
            FROM Company c
            ORDER BY c.createdAt DESC
            """)
    List<Company> findRecent(Pageable pageable);
}