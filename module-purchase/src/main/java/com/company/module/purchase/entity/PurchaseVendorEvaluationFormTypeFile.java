package com.company.module.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "purchase_vendor_evaluation_form_type_file", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_VENDOR_EVAL_FORM_FILE_TYPE", columnNames = "PURCHASE_VENDOR_EVALUATION_FORM_TYPE_ID")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseVendorEvaluationFormTypeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_VENDOR_EVALUATION_FORM_TYPE_FILE_ID")
    private Long purchaseVendorEvaluationFormTypeFileId;

    @Column(name = "PURCHASE_VENDOR_EVALUATION_FORM_TYPE_ID", nullable = false)
    private Long purchaseVendorEvaluationFormTypeId;

    @Lob
    @Column(name = "FILE_DATA", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Builder
    private PurchaseVendorEvaluationFormTypeFile(Long purchaseVendorEvaluationFormTypeId, byte[] fileData, Long createdBy) {
        this.purchaseVendorEvaluationFormTypeId = purchaseVendorEvaluationFormTypeId;
        this.fileData = fileData;
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
