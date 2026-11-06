package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.SkillTestRatingThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillTestRatingThresholdRepository extends JpaRepository<SkillTestRatingThreshold, Long> {

    /**
     * Lấy tất cả ngưỡng xếp loại, sắp xếp giảm dần theo minScore.
     * Dùng để tra cứu xếp loại: lấy dòng đầu tiên có minScore <= score.
     */
    List<SkillTestRatingThreshold> findAllByOrderByMinScoreDesc();

    /**
     * Lấy ngưỡng xếp loại phù hợp nhất với điểm cho trước.
     * Tìm bản ghi có minScore cao nhất mà vẫn <= score.
     */
    @Query("SELECT t FROM SkillTestRatingThreshold t WHERE t.minScore <= :score ORDER BY t.minScore DESC")
    List<SkillTestRatingThreshold> findMatchingThresholds(double score);

    /**
     * Lấy tất cả ngưỡng theo thứ tự hiển thị cho trang Admin.
     */
    List<SkillTestRatingThreshold> findAllByOrderByDisplayOrderAsc();
}
