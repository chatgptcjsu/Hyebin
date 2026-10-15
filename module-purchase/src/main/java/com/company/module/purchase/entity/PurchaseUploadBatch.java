package com.company.module.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_upload_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseUploadBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UPLOAD_BATCH_ID")
    private Long uploadBatchId;

    @Column(name = "PURCHASE_YEAR", nullable = false)
    private Integer purchaseYear;

    @Column(name = "PURCHASE_MONTH", nullable = false)
    private Integer purchaseMonth;

    @Column(name = "ORIGINAL_FILENAME", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "FILE_SHA256", nullable = false, length = 64)
    private String fileSha256;

    @Column(name = "UPLOAD_STATUS", nullable = false, length = 20)
    private String uploadStatus;

    @Column(name = "PLAN_AMOUNT", nullable = false, precision = 18, scale = 6)
    private BigDecimal planAmount;

    @Column(name = "ACTUAL_AMOUNT", nullable = false, precision = 18, scale = 6)
    private BigDecimal actualAmount;

    @Column(name = "DETAIL_ROW_COUNT", nullable = false)
    private Integer detailRowCount;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY")
    private Long updatedBy;

    @Column(name = "DELETED_YN", nullable = false, length = 1)
    private String deletedYn;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "DELETED_BY")
    private Long deletedBy;

    @Builder
    private PurchaseUploadBatch(Integer purchaseYear, Integer purchaseMonth, String originalFilename,
                                String fileSha256, BigDecimal planAmount, BigDecimal actualAmount,
                                Integer detailRowCount, Long createdBy) {
        this.purchaseYear = purchaseYear;
        this.purchaseMonth = purchaseMonth;
        this.originalFilename = originalFilename;
        this.fileSha256 = fileSha256;
        this.planAmount = planAmount;
        this.actualAmount = actualAmount;
        this.detailRowCount = detailRowCount;
        this.uploadStatus = "CURRENT";
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

    public void markReplaced(Long updatedBy) {
        this.uploadStatus = "REPLACED";
        this.updatedBy = updatedBy;
    }

    public void softDelete(Long deletedBy) {
        this.deletedYn = "Y";
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.updatedBy = deletedBy;
    }
}
