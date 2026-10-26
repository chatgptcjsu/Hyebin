package com.company.module.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "purchase_upload_file", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_UPLOAD_FILE_BATCH", columnNames = "UPLOAD_BATCH_ID")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseUploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_UPLOAD_FILE_ID")
    private Long purchaseUploadFileId;

    @Column(name = "UPLOAD_BATCH_ID", nullable = false)
    private Long uploadBatchId;

    @Lob
    @Column(name = "FILE_DATA", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

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
    private PurchaseUploadFile(Long uploadBatchId, byte[] fileData, Long createdBy) {
        this.uploadBatchId = uploadBatchId;
        this.fileData = fileData;
        this.createdBy = createdBy;
        this.deletedYn = "N";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        if (this.deletedYn == null) this.deletedYn = "N";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
