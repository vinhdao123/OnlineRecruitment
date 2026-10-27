package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.SkillTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillTestRepository extends JpaRepository<SkillTest, Long> {

    @Query("""
        SELECT t FROM SkillTest t
        WHERE t.status = 'ACTIVE'
          AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(t.skillName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR t.category = :category)
        ORDER BY t.participantCount DESC, t.id DESC
    """)
    Page<SkillTest> searchActive(@Param("keyword") String keyword,
                                 @Param("category") String category,
                                 Pageable pageable);

    @Query("""
        SELECT t FROM SkillTest t
        WHERE (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(t.skillName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR t.category = :category)
        ORDER BY t.id DESC
    """)
    Page<SkillTest> searchAllAdmin(@Param("keyword") String keyword,
                                   @Param("category") String category,
                                   Pageable pageable);

    @Query("SELECT DISTINCT t.category FROM SkillTest t WHERE t.status = 'ACTIVE'")
    List<String> findAllCategories();

    List<SkillTest> findTop6ByStatusOrderByParticipantCountDesc(String status);
}
