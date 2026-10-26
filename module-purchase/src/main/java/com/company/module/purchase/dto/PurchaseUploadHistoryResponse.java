package com.company.module.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PurchaseUploadHistoryResponse {
    private Long uploadBatchId;
    private Integer purchaseYear;
    private Integer purchaseMonth;
    private String originalFilename;
    private String uploadStatus;
    private boolean originalFileAvailable;
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUploadedAt;
}
