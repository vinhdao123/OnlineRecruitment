package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.JobStatus;
import OnlineRecruitment.OnlineRecruitment.repository.JobRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// Tự động đóng các tin ACTIVE đã quá hạn nộp hồ sơ (deadline)
@Component
@RequiredArgsConstructor
public class JobAutoCloseScheduler {

    private final JobRepository jobRepository;

    // Chạy 30 giây sau khi app khởi động, rồi lặp lại mỗi 1 giờ
    @Scheduled(initialDelay = 30_000, fixedRate = 3_600_000)
    @Transactional
    // Đóng expired jobs trong luồng tuyển dụng.
    public void closeExpiredJobs() {
        // Tin có deadline < hôm nay là đã hết hạn (đúng ngày deadline vẫn còn hiển thị)
        List<Job> expired = jobRepository
                .findByStatusAndDeadlineBefore(
                JobStatus.ACTIVE,
                LocalDate.now()
        );

        // Chuyển từng tin sang CLOSED (updated_at tự cập nhật nhờ @PreUpdate)
        for (Job job : expired) {
            job.setStatus(JobStatus.CLOSED);
        }

        jobRepository.saveAll(expired);
    }
}