package OnlineRecruitment.OnlineRecruitment.dto;

import java.time.LocalDateTime;

public class AdminUserViewDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    public AdminUserViewDto(Long id,
                            String username,
                            String email,
                            String fullName,
                            String phone,
                            String role,
                            String status,
                            LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}