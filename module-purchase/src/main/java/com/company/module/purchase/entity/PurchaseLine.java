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
@Table(name = "purchase_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_LINE_ID")
    private Long purchaseLineId;

    @Column(name = "UPLOAD_BATCH_ID", nullable = false)
    private Long uploadBatchId;

    @Column(name = "SOURCE_ROW_NO", nullable = false)
    private Integer sourceRowNo;

    @Column(name = "BUSINESS_UNIT", length = 50)
    private String businessUnit;

    @Column(name = "IMPORT_TYPE", length = 20)
    private String importType;

    @Column(name = "SUPPLIER_CODE", length = 50)
    private String supplierCode;

    @Column(name = "UNIT_NAME", length = 30)
    private String unitName;

    @Column(name = "PREVIOUS_QUANTITY", precision = 18, scale = 4)
    private BigDecimal previousQuantity;

    @Column(name = "PREVIOUS_AMOUNT", precision = 18, scale = 6)
    private BigDecimal previousAmount;

    @Column(name = "PLAN_QUANTITY", precision = 18, scale = 4)
    private BigDecimal planQuantity;

    @Column(name = "PLAN_AMOUNT", precision = 18, scale = 6)
    private BigDecimal planAmount;

    @Column(name = "ACTUAL_QUANTITY", precision = 18, scale = 4)
    private BigDecimal actualQuantity;

    @Column(name = "ACTUAL_AMOUNT", precision = 18, scale = 6)
    private BigDecimal actualAmount;

    @Column(name = "NOTE_TEXT", columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "SUPPLIER_NAME", nullable = false, length = 200)
    private String supplierName;

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
    private PurchaseLine(Long uploadBatchId, Integer sourceRowNo, String businessUnit, String importType,
                         String supplierCode, String unitName, BigDecimal previousQuantity,
                         BigDecimal previousAmount, BigDecimal planQuantity, BigDecimal planAmount,
                         BigDecimal actualQuantity, BigDecimal actualAmount, String noteText,
                         String supplierName, Long createdBy) {
        this.uploadBatchId = uploadBatchId;
        this.sourceRowNo = sourceRowNo;
        this.businessUnit = businessUnit;
        this.importType = importType;
        this.supplierCode = supplierCode;
        this.unitName = unitName;
        this.previousQuantity = previousQuantity;
        this.previousAmount = previousAmount;
        this.planQuantity = planQuantity;
        this.planAmount = planAmount;
        this.actualQuantity = actualQuantity;
        this.actualAmount = actualAmount;
        this.noteText = noteText;
        this.supplierName = supplierName;
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

    public void softDelete(Long deletedBy) {
        this.deletedYn = "Y";
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.updatedBy = deletedBy;
    }
}
