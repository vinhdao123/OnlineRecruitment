
package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.repository.Recruiter.InterviewRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InterviewService {

    private final InterviewRepository interviewRepository;

    public InterviewService(InterviewRepository interviewRepository) {
        this.interviewRepository = interviewRepository;
    }

    // Tạo lịch phỏng vấn cho nhà tuyển dụng
    @Transactional
    public int createAvailability(
            Long recruiterId,
            Long companyId,
            List<LocalDate> dates,
            LocalTime startTime,
            LocalTime endTime,
            Integer durationMinutes
    ) {

        // 1. Kiểm tra nhà tuyển dụng và công ty
        if (recruiterId == null || companyId == null) {
            throw new IllegalArgumentException(
                    "Không xác định được nhà tuyển dụng hoặc công ty."
            );
        }

        // 2. Kiểm tra ngày được chọn
        if (dates == null || dates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ít nhất một ngày mở lịch."
            );
        }

        // 3. Kiểm tra giờ
        if (startTime == null || endTime == null
                || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "Giờ kết thúc phải sau giờ bắt đầu."
            );
        }

        // 4. Kiểm tra thời lượng
        Set<Integer> allowedDurations = Set.of(
                15, 30, 45, 60, 90, 120
        );

        if (durationMinutes == null
                || !allowedDurations.contains(durationMinutes)) {
            throw new IllegalArgumentException(
                    "Thời lượng phải là 15, 30, 45, 60, 90 hoặc 120 phút."
            );
        }

        // 5. Kiểm tra tổng thời gian có chia hết cho thời lượng không
        long totalMinutes = ChronoUnit.MINUTES.between(
                startTime,
                endTime
        );

        if (totalMinutes % durationMinutes != 0) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian đã chọn phải chia hết cho "
                            + durationMinutes + " phút."
            );
        }

        // 6. Kiểm tra ngày không được nằm trong quá khứ
        for (LocalDate date : dates) {

            if (date == null || date.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "Không được mở lịch trong ngày đã qua."
                );
            }
        }

        // 7. Loại bỏ ngày bị chọn trùng
        List<LocalDate> uniqueDates = new ArrayList<>(
                new LinkedHashSet<>(dates)
        );

        // 8. Không cho tạo lại ngày đã có lịch
        List<LocalDate> existingDates =
                interviewRepository.findExistingDates(
                        recruiterId,
                        companyId
                );

        Set<LocalDate> existingDateSet =
                new LinkedHashSet<>(existingDates);

//        for (LocalDate date : uniqueDates) {
//
//            if (existingDateSet.contains(date)) {
//                throw new IllegalArgumentException(
//                        "Ngày " + date
//                                + " đã có lịch phỏng vấn. "
//                                + "Vui lòng chọn ngày khác."
//                );
//            }
//        }

        // 9. Tạo các khung giờ theo thời lượng đã chọn
        int inserted = 0;

        for (LocalDate date : uniqueDates) {

            // Ngày đã có lịch thì bỏ qua, không tạo trùng
            if (existingDateSet.contains(date)) {
                continue;
            }

            LocalTime current = startTime;

            while (current.plusMinutes(durationMinutes)
                    .compareTo(endTime) <= 0) {

                LocalTime next = current.plusMinutes(durationMinutes);

                int result = interviewRepository.insertAvailability(
                        recruiterId,
                        companyId,
                        date,
                        current,
                        next
                );

                inserted += result;

                current = next;
            }
        }

        return inserted;
    }

    // Lấy danh sách lịch nhà tuyển dụng đã mở
    public List<Map<String, Object>> getAvailability(
            Long recruiterId,
            Long companyId
    ) {

        return interviewRepository.findAvailability(
                recruiterId,
                companyId
        );
    }
}
