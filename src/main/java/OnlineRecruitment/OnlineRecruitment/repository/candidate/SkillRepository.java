package OnlineRecruitment.OnlineRecruitment.repository.candidate;

import OnlineRecruitment.OnlineRecruitment.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByStatusOrderByNameAsc(String status);

    // Sắp xếp toàn bộ kỹ năng theo tên để trang Admin dễ theo dõi.
    List<Skill> findAllByOrderByNameAsc();

    // Không cho phép trùng kỹ năng trong cùng danh mục.
    boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
    boolean existsByNameIgnoreCaseAndCategoryIdAndIdNot(String name, Long categoryId, Long id);
}