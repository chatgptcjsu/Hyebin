package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseUploadHistoryResponse;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseUploadHistoryService {

    private final PurchaseUploadBatchRepository batchRepository;

    public List<PurchaseUploadHistoryResponse> getHistory(Integer year) {
        if (year == null || year < 2000 || year > 2200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return batchRepository.findByPurchaseYearAndDeletedYnOrderByCreatedAtDesc(year, "N")
            .stream()
            .map(batch -> PurchaseUploadHistoryResponse.builder()
                .uploadBatchId(batch.getUploadBatchId())
                .purchaseYear(batch.getPurchaseYear())
                .purchaseMonth(batch.getPurchaseMonth())
                .originalFilename(batch.getOriginalFilename())
                .uploadStatus(batch.getUploadStatus())
                .planAmount(batch.getPlanAmount())
                .actualAmount(batch.getActualAmount())
                .createdAt(batch.getCreatedAt())
                .build())
            .toList();
    }
}
