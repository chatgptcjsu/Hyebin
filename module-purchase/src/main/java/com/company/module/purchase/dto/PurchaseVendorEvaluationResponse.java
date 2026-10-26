package com.company.module.purchase.dto;

import com.company.module.purchase.service.PurchaseVendorEvaluationScoring;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class PurchaseVendorEvaluationResponse {
    private Long purchaseVendorEvaluationId;
    private Integer evaluationYear;
    private String supplierName;
    private String evaluatorName;
    private LocalDate evaluationDate;
    private Map<String, String> answers;
    private List<PurchaseVendorEvaluationScoring.ScoredCriterion> criteriaScores;
    private BigDecimal totalScore;
    private String finalGrade;
}
