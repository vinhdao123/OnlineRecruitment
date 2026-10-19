package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.ActivityLog;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.admin.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Ghi 1 dòng activity log.
     * actor: user thực hiện (có thể null nếu system)
     */
    @Transactional
    public void log(User actor, String action, String description,
                    String referenceType, Long referenceId, String statusTag) {
        ActivityLog log = new ActivityLog();
        log.setUser(actor);
        log.setAction(action);
        log.setDescription(description);
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setStatusTag(statusTag);
        activityLogRepository.save(log);
    }

    /** Lấy N activity gần nhất cho dashboard. */
    @Transactional(readOnly = true)
    public List<ActivityLog> recent(int limit) {
        return activityLogRepository.findRecent(PageRequest.of(0, limit));
    }

    /** Phân trang activity cho activity log screen. */
    @Transactional(readOnly = true)
    public Page<ActivityLog> page(int page, int size) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
    }
}