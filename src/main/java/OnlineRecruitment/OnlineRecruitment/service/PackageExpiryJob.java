package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.UserPackage;
import OnlineRecruitment.OnlineRecruitment.repository.UserPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PackageExpiryJob {

    private final UserPackageRepository userPackageRepository;

    /** Chạy 0h mỗi ngày: quét các gói ACTIVE đã quá endDate → EXPIRED. */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireOverduePackages() {
        LocalDateTime now = LocalDateTime.now();
        List<UserPackage> actives = userPackageRepository.findAll()
                .stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getEndDate().isBefore(now))
                .toList();
        actives.forEach(p -> p.setStatus("EXPIRED"));
        userPackageRepository.saveAll(actives);
        System.out.println("[PackageExpiryJob] Đã hết hạn " + actives.size() + " gói.");
    }
}