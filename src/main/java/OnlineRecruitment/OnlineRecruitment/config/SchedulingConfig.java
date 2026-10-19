package OnlineRecruitment.OnlineRecruitment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Bật chức năng @Scheduled cho toàn ứng dụng (không cần sửa class chính)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}