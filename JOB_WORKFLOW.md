# Quy trình quản lý tin, danh mục và kỹ năng

## Trạng thái tin tuyển dụng

| Thao tác | Trạng thái đầu | Trạng thái sau | Điều kiện |
|---|---|---|---|
| Lưu nháp | Mới, DRAFT, REJECTED, PENDING, ACTIVE | DRAFT | Có tiêu đề, danh mục hoạt động; thông tin đã nhập phải hợp lệ |
| Gửi duyệt | DRAFT, REJECTED hoặc form đang sửa | PENDING | Đủ thông tin; công ty và recruiter hoạt động; danh mục hoạt động; hạn không trước hôm nay |
| Admin duyệt | PENDING | ACTIVE | Kiểm tra lại đầy đủ điều kiện ngay lúc duyệt |
| Admin từ chối | PENDING | REJECTED | Lý do từ 1 đến 500 ký tự |
| Recruiter đóng | ACTIVE | CLOSED | Đúng chủ sở hữu |
| Hết hạn | ACTIVE | EXPIRED | Deadline trước hôm nay; ngày deadline vẫn được nhận hồ sơ |
| Soạn đăng lại | CLOSED, EXPIRED | DRAFT | Recruiter sở hữu tin hoặc admin |

Tin đăng lại giữ nguyên ID, công ty, nội dung và các hồ sơ ứng tuyển trước đây. Không tự động thay hạn và không tự động hiển thị: recruiter cập nhật hạn rồi gửi duyệt lại. Sửa tin ACTIVE/PENDING và lưu nháp sẽ rút tin khỏi hiển thị/hàng đợi; nút gửi duyệt mới đưa tin vào PENDING.

Thông tin bắt buộc khi gửi/duyệt: tiêu đề, mô tả, danh mục, địa điểm, yêu cầu ứng viên, hình thức làm việc, số lượng dương và hạn ứng tuyển. Lương có thể thỏa thuận; nếu nhập thì phải không âm và min không lớn hơn max. Đơn vị VND hoặc USD.

Trang recruiter chỉ hiển thị/lọc/đếm tin của tài khoản đang đăng nhập. Kho tin có bốn tab: nháp, đã đóng, quá hạn, bị từ chối. Trang admin có bộ lọc cho cả sáu trạng thái. Chỉ PENDING có nút duyệt/từ chối; CLOSED/EXPIRED có nút cho phép soạn đăng lại.

## Danh mục và kỹ năng

- `/admin/categories`: tìm kiếm, lọc, tạo, sửa, ngừng dùng/kích hoạt lại danh mục.
- `/admin/skills`: tìm kiếm, lọc, tạo, sửa, ngừng dùng/kích hoạt lại kỹ năng.
- Chuẩn hóa khoảng trắng; kiểm tra tên trùng không phân biệt hoa thường, kể cả bản ghi ngừng dùng.
- Không xóa bản ghi đang được tham chiếu. Kích hoạt lại giữ nguyên ID.
- Danh mục INACTIVE không được chọn khi lưu/gửi tin, và chặn duyệt các tin còn chờ. Tin đã đăng giữ danh mục cũ.
- Skill được phân loại theo Category theo bản cũ. Kỹ năng cũ chưa phân loại vẫn đọc được trong CV. Tạo/kích hoạt kỹ năng cần Category ACTIVE; kỹ năng đã dùng trong `job_skills` không được chuyển Category. Không thay đổi các liên kết Job–Skill hiện có.

## Kiểm tra

Chạy các kiểm thử độc lập, không khởi động Spring context và không ghi vào database:

```text
mvn test -Dtest=JobWorkflowTest,JobPagesTest,JobCategoryAdminServiceTest,AdminSkillServiceTest
```

`JobPagesTest` render template bằng Thymeleaf để kiểm tra sidebar dùng chung, trang Job, form Category/Skill và nút theo trạng thái. Kiểm thử service dùng repository giả lập; chưa phải kiểm thử thao tác ghi thực tế trên SQL Server.

## Ghép admin từ dự án cũ

Nguồn: `OnlineRecruitment_Admin_Category_Sidebar_Fixed/OnlineRecruitment_latest`.

- Lấy template Job, Category, Skill và các CSS phụ thuộc từ bản cũ; ghép sidebar vào `_layout.html` của bản main để giữ menu doanh nghiệp và các module admin hiện tại.
- `/admin/jobs` mặc định xem tất cả; sidebar có chờ duyệt, đang tuyển, từ chối, nháp, đóng và quá hạn. Duyệt tin vẫn kiểm tra theo quy tắc nghiệp vụ ở trên.
- Dùng controller/service quản lý Skill của bản cũ và bổ sung kiểm tra lỗi database cùng bộ lọc tên, trạng thái, Category.
- Đã kiểm tra metadata SQL Server cục bộ: có `skills.category_id`, bảng `job_skills`, constraint `CK_jobs_status` hỗ trợ EXPIRED, và UNIQUE `skills.name`. Vì vậy giữ tên kỹ năng duy nhất toàn hệ thống thay vì chỉ kiểm tra trong từng Category như bản cũ. Không chạy ALTER/UPDATE database khi ghép.
- Các bản database khác cần cùng schema. `ddl-auto=none` nên ứng dụng không tự tạo cột còn thiếu.
