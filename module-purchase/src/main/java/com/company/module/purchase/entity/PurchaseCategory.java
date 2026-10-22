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
@Table(name = "purchase_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_CATEGORY_ID")
    private Long purchaseCategoryId;

    @Column(name = "UPLOAD_BATCH_ID", nullable = false)
    private Long uploadBatchId;

    @Column(name = "SOURCE_ROW_NO", nullable = false)
    private Integer sourceRowNo;

    @Column(name = "CATEGORY_NAME", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "PREVIOUS_ACTUAL_AMOUNT", precision = 24, scale = 10)
    private BigDecimal previousActualAmount;

    @Column(name = "PLAN_FOREIGN_AMOUNT", precision = 24, scale = 10)
    private BigDecimal planForeignAmount;

    @Column(name = "PLAN_DOMESTIC_AMOUNT", precision = 24, scale = 10)
    private BigDecimal planDomesticAmount;

    @Column(name = "PLAN_AMOUNT", precision = 24, scale = 10)
    private BigDecimal planAmount;

    @Column(name = "ACTUAL_FOREIGN_AMOUNT", precision = 24, scale = 10)
    private BigDecimal actualForeignAmount;

    @Column(name = "ACTUAL_DOMESTIC_AMOUNT", precision = 24, scale = 10)
    private BigDecimal actualDomesticAmount;

    @Column(name = "ACTUAL_AMOUNT", precision = 24, scale = 10)
    private BigDecimal actualAmount;

    @Column(name = "PLAN_VARIANCE_AMOUNT", precision = 24, scale = 10)
    private BigDecimal planVarianceAmount;

    @Column(name = "PREVIOUS_MONTH_VARIANCE_AMOUNT", precision = 24, scale = 10)
    private BigDecimal previousMonthVarianceAmount;

    @Column(name = "PLAN_VARIANCE_REASON", columnDefinition = "TEXT")
    private String planVarianceReason;

    @Column(name = "PREVIOUS_MONTH_VARIANCE_REASON", columnDefinition = "TEXT")
    private String previousMonthVarianceReason;

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
    private PurchaseCategory(Long uploadBatchId, Integer sourceRowNo, String categoryName,
                             BigDecimal previousActualAmount, BigDecimal planForeignAmount,
                             BigDecimal planDomesticAmount, BigDecimal planAmount,
                             BigDecimal actualForeignAmount, BigDecimal actualDomesticAmount,
                             BigDecimal actualAmount, BigDecimal planVarianceAmount,
                             BigDecimal previousMonthVarianceAmount, String planVarianceReason,
                             String previousMonthVarianceReason, Long createdBy) {
        this.uploadBatchId = uploadBatchId;
        this.sourceRowNo = sourceRowNo;
        this.categoryName = categoryName;
        this.previousActualAmount = previousActualAmount;
        this.planForeignAmount = planForeignAmount;
        this.planDomesticAmount = planDomesticAmount;
        this.planAmount = planAmount;
        this.actualForeignAmount = actualForeignAmount;
        this.actualDomesticAmount = actualDomesticAmount;
        this.actualAmount = actualAmount;
        this.planVarianceAmount = planVarianceAmount;
        this.previousMonthVarianceAmount = previousMonthVarianceAmount;
        this.planVarianceReason = planVarianceReason;
        this.previousMonthVarianceReason = previousMonthVarianceReason;
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
