package com.company.module.purchase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "purchase_vendor_evaluation_form_type", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_VENDOR_EVAL_FORM_TYPE_NAME", columnNames = "FORM_TYPE_NAME")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseVendorEvaluationFormType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_VENDOR_EVALUATION_FORM_TYPE_ID")
    private Long purchaseVendorEvaluationFormTypeId;

    @Column(name = "FORM_TYPE_NAME", nullable = false, length = 100)
    private String formTypeName;

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
    private PurchaseVendorEvaluationFormType(String formTypeName, Long createdBy) {
        this.formTypeName = formTypeName;
        this.createdBy = createdBy;
        this.deletedYn = "N";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        if (this.deletedYn == null) this.deletedYn = "N";
    }
}
