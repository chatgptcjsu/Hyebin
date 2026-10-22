package com.company.module.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_month_snapshot", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_MONTH", columnNames = {"PURCHASE_YEAR", "PURCHASE_MONTH"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseMonthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SNAPSHOT_ID")
    private Long snapshotId;

    @Column(name = "PURCHASE_YEAR", nullable = false)
    private Integer purchaseYear;

    @Column(name = "PURCHASE_MONTH", nullable = false)
    private Integer purchaseMonth;

    @Column(name = "CURRENT_UPLOAD_BATCH_ID", nullable = false)
    private Long currentUploadBatchId;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private Long updatedBy;

    @Column(name = "DELETED_YN", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String deletedYn;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "DELETED_BY")
    private Long deletedBy;

    @Builder
    private PurchaseMonthSnapshot(Integer purchaseYear, Integer purchaseMonth,
                                  Long currentUploadBatchId, Long createdBy) {
        this.purchaseYear = purchaseYear;
        this.purchaseMonth = purchaseMonth;
        this.currentUploadBatchId = currentUploadBatchId;
        this.createdBy = createdBy;
        this.deletedYn = "N";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.deletedYn == null) this.deletedYn = "N";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void replaceBatch(Long currentUploadBatchId, Long updatedBy) {
        this.currentUploadBatchId = currentUploadBatchId;
        this.updatedBy = updatedBy;
    }

    public void softDelete(Long deletedBy) {
        this.deletedYn = "Y";
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.updatedBy = deletedBy;
    }
}
