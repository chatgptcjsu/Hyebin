package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseDetailSheetResponse;
import com.company.module.purchase.entity.PurchaseDetailSheet;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.repository.PurchaseDetailSheetRepository;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseDetailSheetService {

    private final PurchaseMonthSnapshotRepository snapshotRepository;
    private final PurchaseDetailSheetRepository detailSheetRepository;
    private final ObjectMapper objectMapper;

    public PurchaseDetailSheetResponse getCurrentSheet(Integer year, Integer month) {
        if (year == null || year < 2000 || year > 2200 || month == null || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PurchaseMonthSnapshot snapshot = snapshotRepository
            .findByPurchaseYearAndPurchaseMonthAndDeletedYn(year, month, "N")
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        PurchaseDetailSheet stored = detailSheetRepository
            .findByUploadBatchIdAndDeletedYn(snapshot.getCurrentUploadBatchId(), "N")
            .orElse(null);
        if (stored == null) {
            return new PurchaseDetailSheetResponse(
                false,
                "이 자료는 두 번째 시트 상세가 저장되기 전에 업로드되었습니다. 원본 Excel 파일을 다시 업로드해 주세요.",
                year, month, null, null, null, null, null, null, null,
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()
            );
        }
        try {
            ParsedPurchaseDetailSheet sheet = objectMapper.readValue(stored.getSheetDataJson(), ParsedPurchaseDetailSheet.class);
            return new PurchaseDetailSheetResponse(
                true, null, year, month, sheet.sheetName(), sheet.title(), sheet.sectionTitle(), sheet.unitLabel(),
                sheet.previousMonthLabel(), sheet.planMonthLabel(), sheet.actualMonthLabel(),
                sheet.rows(), sheet.summaryRows(), sheet.mainIssues(), sheet.futureImprovements()
            );
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
