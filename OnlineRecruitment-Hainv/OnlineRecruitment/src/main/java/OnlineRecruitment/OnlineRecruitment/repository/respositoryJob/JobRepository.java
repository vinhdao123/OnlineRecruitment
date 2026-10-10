
package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

import OnlineRecruitment.OnlineRecruitment.entity.entityjob.Job2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job2, Long> {

    // ================= TRANG CÔNG KHAI =================

    @Query("""
            SELECT j FROM Job2 j
            JOIN j.company c
            WHERE j.status = 'APPROVED'
              AND (j.deadline IS NULL OR j.deadline >= CURRENT_DATE)
              AND (
                    LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.companyName)
                       LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
              AND (:location = '' OR j.location = :location)
            ORDER BY j.createdAt DESC
            """)
    Page<Job2> search(
            @Param("keyword") String keyword,
            @Param("location") String location,
            Pageable pageable
    );

    // ================= THỐNG KÊ THEO TRẠNG THÁI =================

    // Chỉ khai báo một lần để tránh lỗi trùng phương thức.
    long countByStatus(String status);

    // ================= DANH SÁCH ĐỊA ĐIỂM =================

    @Query("""
            SELECT DISTINCT j.location
            FROM Job2 j
            WHERE j.status = 'APPROVED'
              AND j.location IS NOT NULL
            ORDER BY j.location
            """)
    List<String> findAllLocations();

    // ================= ADMIN DUYỆT TIN =================

    @Query("""
            SELECT j FROM Job2 j
            JOIN j.company c
            WHERE (:status = '' OR j.status = :status)
              AND (
                    LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(c.companyName)
                       LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY j.createdAt DESC
            """)
    Page<Job2> findForReview(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );

    // ================= TIN CỦA NHÀ TUYỂN DỤNG =================

    @Query("""
            SELECT j FROM Job2 j
            WHERE j.recruiterId = :recruiterId
              AND (:status = '' OR j.status = :status)
              AND LOWER(j.title)
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY j.createdAt DESC
            """)
    Page<Job2> findMine(
            @Param("recruiterId") Long recruiterId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}