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
@Table(name = "purchase_detail_sheet", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_DETAIL_BATCH", columnNames = "UPLOAD_BATCH_ID")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseDetailSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_DETAIL_SHEET_ID")
    private Long purchaseDetailSheetId;

    @Column(name = "UPLOAD_BATCH_ID", nullable = false)
    private Long uploadBatchId;

    @Column(name = "SHEET_NAME", nullable = false, length = 128)
    private String sheetName;

    @Column(name = "SHEET_DATA_JSON", nullable = false, columnDefinition = "LONGTEXT")
    private String sheetDataJson;

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
    private PurchaseDetailSheet(Long uploadBatchId, String sheetName, String sheetDataJson, Long createdBy) {
        this.uploadBatchId = uploadBatchId;
        this.sheetName = sheetName;
        this.sheetDataJson = sheetDataJson;
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
}
