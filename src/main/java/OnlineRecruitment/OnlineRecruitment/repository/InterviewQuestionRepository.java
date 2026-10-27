package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.InterviewQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    @Query("""
        SELECT q FROM InterviewQuestion q
        WHERE q.status = 'ACTIVE'
          AND (:keyword IS NULL OR LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(q.purpose) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(q.tips) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(q.sampleAnswer) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR q.category = :category)
        ORDER BY q.displayOrder ASC, q.createdAt DESC
    """)
    Page<InterviewQuestion> searchActive(@Param("keyword") String keyword,
                                         @Param("category") String category,
                                         Pageable pageable);

    @Query("""
        SELECT q FROM InterviewQuestion q
        WHERE (:keyword IS NULL OR LOWER(q.question) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(q.category) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:category IS NULL OR q.category = :category)
        ORDER BY q.displayOrder ASC, q.createdAt DESC
    """)
    Page<InterviewQuestion> searchAllAdmin(@Param("keyword") String keyword,
                                           @Param("category") String category,
                                           Pageable pageable);

    @Query("SELECT DISTINCT q.category FROM InterviewQuestion q WHERE q.status = 'ACTIVE'")
    List<String> findAllCategories();

    List<InterviewQuestion> findTop5ByStatusOrderByViewCountDesc(String status);
}
