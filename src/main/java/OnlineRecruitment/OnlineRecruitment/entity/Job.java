package OnlineRecruitment.OnlineRecruitment.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String requirements;

    private String benefits;

    private String location;

    @Column(name = "salary_min")
    private Double salaryMin;

    @Column(name = "salary_max")
    private Double salaryMax;

    private String experience;

    private String level;

    private Integer quantity;

    private LocalDate deadline;

    private String status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "category_id")
    private Long categoryId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }




    // ================= GETTER =================

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getBenefits() {
        return benefits;
    }

    public String getLocation() {
        return location;
    }

    public Double getSalaryMin() {
        return salaryMin;
    }

    public Double getSalaryMax() {
        return salaryMax;
    }

    public String getExperience() {
        return experience;
    }

    public String getLevel() {
        return level;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    // ================= SETTER =================

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setSalaryMin(Double salaryMin) {
        this.salaryMin = salaryMin;
    }

    public void setSalaryMax(Double salaryMax) {
        this.salaryMax = salaryMax;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}