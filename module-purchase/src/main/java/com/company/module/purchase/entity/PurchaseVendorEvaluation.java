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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "purchase_vendor_evaluation", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PURCHASE_VENDOR_EVAL_YEAR_TYPE_NAME",
        columnNames = {"EVALUATION_YEAR", "EVALUATION_TYPE", "SUPPLIER_NAME"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseVendorEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PURCHASE_VENDOR_EVALUATION_ID")
    private Long purchaseVendorEvaluationId;

    @Column(name = "EVALUATION_YEAR", nullable = false)
    private Integer evaluationYear;

    @Column(name = "EVALUATION_TYPE", nullable = false, length = 100)
    private String evaluationType;

    @Column(name = "SUPPLIER_NAME", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "EVALUATOR_NAME", nullable = false, length = 100)
    private String evaluatorName;

    @Column(name = "EVALUATION_DATE", nullable = false)
    private LocalDate evaluationDate;

    @Column(name = "EVALUATION_OPINION", columnDefinition = "TEXT")
    private String evaluationOpinion;

    @Column(name = "ANSWERS_JSON", nullable = false, columnDefinition = "LONGTEXT")
    private String answersJson;

    @Column(name = "SCORES_JSON", nullable = false, columnDefinition = "LONGTEXT")
    private String scoresJson;

    @Column(name = "TOTAL_SCORE", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalScore;

    @Column(name = "FINAL_GRADE", nullable = false, length = 1)
    private String finalGrade;

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
    private PurchaseVendorEvaluation(Integer evaluationYear, String evaluationType, String supplierName,
                                     String evaluatorName, LocalDate evaluationDate, String evaluationOpinion,
                                     String answersJson, String scoresJson, BigDecimal totalScore, String finalGrade,
                                     Long createdBy) {
        this.evaluationYear = evaluationYear;
        this.evaluationType = evaluationType;
        this.supplierName = supplierName;
        this.evaluatorName = evaluatorName;
        this.evaluationDate = evaluationDate;
        this.evaluationOpinion = evaluationOpinion;
        this.answersJson = answersJson;
        this.scoresJson = scoresJson;
        this.totalScore = totalScore;
        this.finalGrade = finalGrade;
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

    public void updateEvaluation(String evaluationType, String supplierName, String evaluatorName,
                                 LocalDate evaluationDate, String evaluationOpinion, String answersJson,
                                 String scoresJson, BigDecimal totalScore, String finalGrade, Long updatedBy) {
        this.evaluationType = evaluationType;
        this.supplierName = supplierName;
        this.evaluatorName = evaluatorName;
        this.evaluationDate = evaluationDate;
        this.evaluationOpinion = evaluationOpinion;
        this.answersJson = answersJson;
        this.scoresJson = scoresJson;
        this.totalScore = totalScore;
        this.finalGrade = finalGrade;
        this.updatedBy = updatedBy;
    }

    public void softDelete(Long deletedBy) {
        this.deletedYn = "Y";
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.deletedBy = deletedBy;
        this.updatedBy = deletedBy;
    }
}
