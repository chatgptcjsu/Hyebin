package com.company.module.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PurchaseUploadResponse {
    private Long uploadBatchId;
    private Integer purchaseYear;
    private Integer purchaseMonth;
    private String originalFilename;
    private String uploadStatus;
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    private Integer summaryRowCount;
    private boolean alreadyCurrent;
    private boolean requiresReplacementConfirmation;
}
