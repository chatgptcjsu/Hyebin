package com.company.module.purchase.dto;

import com.company.module.purchase.entity.PurchaseUploadBatch;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PurchaseHistoryResponse {
    private Long uploadBatchId;
    private Integer purchaseYear;
    private Integer purchaseMonth;
    private String originalFilename;
    private String uploadStatus;
    private BigDecimal planAmount;
    private BigDecimal actualAmount;
    private Integer detailRowCount;
    private LocalDateTime createdAt;

    public static PurchaseHistoryResponse from(PurchaseUploadBatch batch) {
        return PurchaseHistoryResponse.builder()
            .uploadBatchId(batch.getUploadBatchId())
            .purchaseYear(batch.getPurchaseYear())
            .purchaseMonth(batch.getPurchaseMonth())
            .originalFilename(batch.getOriginalFilename())
            .uploadStatus(batch.getUploadStatus())
            .planAmount(batch.getPlanAmount())
            .actualAmount(batch.getActualAmount())
            .detailRowCount(batch.getDetailRowCount())
            .createdAt(batch.getCreatedAt())
            .build();
    }
}
