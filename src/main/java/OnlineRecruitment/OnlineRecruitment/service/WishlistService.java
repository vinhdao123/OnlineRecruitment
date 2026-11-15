package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.Candidate;
import OnlineRecruitment.OnlineRecruitment.entity.Job;
import OnlineRecruitment.OnlineRecruitment.entity.User;
import OnlineRecruitment.OnlineRecruitment.repository.UserRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.CandidateRepository;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// =====================================================
// SERVICE XỬ LÝ CÔNG VIỆC YÊU THÍCH (WISHLIST) CỦA ỨNG VIÊN
//
// Luồng tổng quát:
//   Controller lấy loginName từ Spring Security
//     -> Service tìm Candidate tương ứng
//     -> Service kiểm tra nghiệp vụ (job tồn tại, đã lưu chưa...)
//     -> WishlistRepository đọc / ghi bảng dbo.wishlist
//     -> Controller nhận kết quả và trả view / redirect
// =====================================================

@Service
public class WishlistService {

    // Kết quả của thao tác bật / tắt yêu thích,
    // Controller dựa vào đây để chọn thông báo hiển thị
    public enum ToggleResult {
        JOB_NOT_FOUND,  // Công việc không tồn tại
        ADDED,          // Đã thêm vào wishlist
        REMOVED         // Đã bỏ khỏi wishlist
    }

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobService jobService;

    public WishlistService(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            CandidateRepository candidateRepository,
            JobService jobService) {

        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.candidateRepository = candidateRepository;
        this.jobService = jobService;
    }

    // ==========================================
    // TÌM CANDIDATE THEO TÊN ĐĂNG NHẬP
    //
    // 1. Tài khoản có thể đăng nhập bằng username hoặc email
    //    -> tìm theo username trước, không thấy thì tìm theo email
    // 2. Không có User -> trả Optional rỗng (Controller sẽ redirect /login)
    // 3. Có User -> lấy Candidate gắn với user_id đó
    // ==========================================

    public Optional<Candidate> findCandidateByLoginName(String loginName) {

        if (loginName == null) {
            return Optional.empty();
        }

        Optional<User> userOptional =
                userRepository.findByUsername(loginName);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(loginName);
        }

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        return candidateRepository.findByUserId(userOptional.get().getId());
    }

    // ==========================================
    // THÊM / XÓA CÔNG VIỆC YÊU THÍCH
    //
    // 1. Kiểm tra công việc có tồn tại không
    //    -> không tồn tại: trả JOB_NOT_FOUND, không ghi DB
    // 2. Kiểm tra cặp (candidate, job) đã có trong wishlist chưa
    // 3. Đã có  -> xóa khỏi wishlist, trả REMOVED
    // 4. Chưa có -> thêm mới vào wishlist, trả ADDED
    //
    // Chạy trong transaction để bước kiểm tra và bước ghi
    // là một khối thống nhất.
    // ==========================================

    @Transactional
    public ToggleResult toggleWishlist(Long candidateId, Long jobId) {

        // 1. Kiểm tra công việc có tồn tại không
        if (jobService.findJobById(jobId).isEmpty()) {
            return ToggleResult.JOB_NOT_FOUND;
        }

        // 2. Kiểm tra công việc đã được yêu thích chưa
        boolean exists =
                wishlistRepository.existsByCandidateIdAndJobId(candidateId, jobId);

        // 3. Nếu đã tồn tại thì xóa
        if (exists) {
            wishlistRepository.remove(candidateId, jobId);
            return ToggleResult.REMOVED;
        }

        // 4. Nếu chưa tồn tại thì thêm mới
        wishlistRepository.add(candidateId, jobId);
        return ToggleResult.ADDED;
    }

    // ==========================================
    // LẤY DANH SÁCH CÔNG VIỆC YÊU THÍCH
    //
    // 1. Lấy danh sách job_id đã lưu (mới lưu xếp trước)
    // 2. Với từng job_id, lấy thông tin chi tiết Job
    //    -> Job đã bị xóa khỏi hệ thống thì bỏ qua
    // 3. Trả danh sách Job cho Controller hiển thị
    // ==========================================

    public List<Job> getWishlistJobs(Long candidateId) {

        // 1. Lấy danh sách ID công việc đã lưu
        List<Long> jobIds =
                wishlistRepository.findJobIdsByCandidateIdOrderByNewest(candidateId);

        // 2. Lấy thông tin chi tiết từng công việc
        List<Job> wishlistJobs = new ArrayList<>();

        for (Long jobId : jobIds) {
            jobService.findJobById(jobId).ifPresent(wishlistJobs::add);
        }

        return wishlistJobs;
    }
}
