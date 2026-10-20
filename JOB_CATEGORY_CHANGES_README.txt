ĐÃ SỬA: CREATE / UPDATE JOB CATEGORY

1. Controller: cập nhật status khi Update; hiển thị lỗi nghiệp vụ.
2. Service: chuẩn hóa tên, kiểm tra trùng, saveAndFlush và chuyển lỗi database thành thông báo; update status không đổi category ID.
3. Form: cho sửa ACTIVE / INACTIVE, chỉ khi update.
4. JOB_CATEGORY_UNIQUE_SQL.sql: kiểm tra trùng và tạo unique index theo tên chuẩn hóa trên SQL Server.
   Vì application.properties đặt ddl-auto=none, phải chạy script SQL trên DB đã backup.
   Kiểm tra các tên trùng trước khi chạy; không tự chỉnh/xóa dữ liệu cũ.

Lưu ý: không chỉnh JobService vì project đã lọc Category ACTIVE và chặn dùng INACTIVE khi đăng tin.
Chưa compile được trong môi trường này: Maven wrapper cần tải Maven nhưng mạng không khả dụng.
