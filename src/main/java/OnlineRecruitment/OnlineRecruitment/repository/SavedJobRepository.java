package OnlineRecruitment.OnlineRecruitment.repository;

import OnlineRecruitment.OnlineRecruitment.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {

    // Danh sách tin đã lưu của một người, mới lưu xếp trước (fetch sẵn job + công ty để không lỗi lazy)
    @Query("select s from SavedJob s join fetch s.job j join fetch j.company " +
            "where s.user.id = :userId order by s.savedAt desc")
    List<SavedJob> findByUserWithJob(@Param("userId") Long userId);

    // Tìm bản ghi đã lưu (để bỏ lưu)
    Optional<SavedJob> findByUser_IdAndJob_Id(Long userId, Long jobId);

    // Kiểm tra đã lưu chưa (để đổi nhãn nút)
    boolean existsByUser_IdAndJob_Id(Long userId, Long jobId);
}