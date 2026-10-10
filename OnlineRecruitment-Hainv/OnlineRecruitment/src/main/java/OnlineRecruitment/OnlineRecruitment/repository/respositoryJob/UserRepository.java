package OnlineRecruitment.OnlineRecruitment.repository.respositoryJob;

import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.entity.UserRole;
import OnlineRecruitment.OnlineRecruitment.entity.UserStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // =========================
    // TÌM KIẾM TÀI KHOẢN
    // =========================

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    // =========================
    // KIỂM TRA TÀI KHOẢN
    // =========================

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // =========================
    // THỐNG KÊ
    // =========================

    long countByRole(UserRole role);

    long countByStatus(UserStatus status);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.createdAt >= :from
          AND u.createdAt < :to
    """)
    long countBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // =========================
    // TÌM KIẾM CÓ PHÂN TRANG
    // =========================

    @Query("""
        SELECT u FROM User u
        WHERE (
            :keyword IS NULL
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.status = :status)
    """)
    Page<User> search(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    // =========================
    // DANH SÁCH USER MỚI NHẤT
    // =========================

    @Query("""
        SELECT u FROM User u
        ORDER BY u.createdAt DESC
    """)
    List<User> findRecent(Pageable pageable);

    // =========================
    // TÌM KIẾM TOÀN BỘ - EXPORT CSV
    // =========================

    @Query("""
        SELECT u FROM User u
        WHERE (
            :keyword IS NULL
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:role IS NULL OR u.role = :role)
        AND (:status IS NULL OR u.status = :status)
        ORDER BY u.createdAt DESC
    """)
    List<User> searchAll(
            @Param("keyword") String keyword,
            @Param("role") UserRole role,
            @Param("status") UserStatus status
    );
}