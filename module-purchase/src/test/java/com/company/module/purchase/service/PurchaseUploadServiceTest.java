package com.company.module.purchase.service;

import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.repository.PurchaseCategoryRepository;
import com.company.module.purchase.repository.PurchaseDetailSheetRepository;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import com.company.module.purchase.repository.PurchaseUploadFileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseUploadServiceTest {

    @Mock private PurchaseExcelParser excelParser;
    @Mock private PurchaseUploadBatchRepository batchRepository;
    @Mock private PurchaseMonthSnapshotRepository snapshotRepository;
    @Mock private PurchaseCategoryRepository categoryRepository;
    @Mock private PurchaseDetailSheetRepository detailSheetRepository;
    @Mock private PurchaseUploadFileRepository uploadFileRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private CurrentUserProvider currentUserProvider;
    @InjectMocks private PurchaseUploadService uploadService;

    @Test
    void identicalFileIsConfirmedAndSavedAsANewCurrentVersion() throws Exception {
        byte[] content = "same original workbook".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "2026년 7월 구매실적자료.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
        ParsedPurchaseWorkbook parsed = new ParsedPurchaseWorkbook(
            2026, 7, new BigDecimal("100"), new BigDecimal("95"), List.of(), null);
        PurchaseMonthSnapshot current = org.mockito.Mockito.mock(PurchaseMonthSnapshot.class);
        PurchaseUploadBatch previousBatch = org.mockito.Mockito.mock(PurchaseUploadBatch.class);
        PurchaseUploadBatch newBatch = org.mockito.Mockito.mock(PurchaseUploadBatch.class);
        when(excelParser.parse(file)).thenReturn(parsed);
        when(snapshotRepository.findByPurchaseYearAndPurchaseMonthAndDeletedYn(2026, 7, "N"))
            .thenReturn(Optional.of(current));
        when(current.getCurrentUploadBatchId()).thenReturn(10L);
        when(batchRepository.findById(10L)).thenReturn(Optional.of(previousBatch));

        var pending = uploadService.upload(file, false);

        assertTrue(pending.isRequiresReplacementConfirmation());
        assertFalse(pending.isAlreadyCurrent());
        verify(batchRepository, never()).save(any(PurchaseUploadBatch.class));

        when(currentUserProvider.getCurrentUserId()).thenReturn(99L);
        when(batchRepository.save(any(PurchaseUploadBatch.class))).thenReturn(newBatch);
        when(newBatch.getUploadBatchId()).thenReturn(11L);
        when(newBatch.getPurchaseYear()).thenReturn(2026);
        when(newBatch.getPurchaseMonth()).thenReturn(7);
        when(newBatch.getOriginalFilename()).thenReturn(file.getOriginalFilename());
        when(newBatch.getUploadStatus()).thenReturn("CURRENT");
        when(newBatch.getPlanAmount()).thenReturn(parsed.planAmount());
        when(newBatch.getActualAmount()).thenReturn(parsed.actualAmount());
        when(newBatch.getSummaryRowCount()).thenReturn(0);

        var saved = uploadService.upload(file, true);

        assertEquals(11L, saved.getUploadBatchId());
        assertFalse(saved.isAlreadyCurrent());
        assertFalse(saved.isRequiresReplacementConfirmation());
        verify(batchRepository).save(any(PurchaseUploadBatch.class));
        verify(previousBatch).markReplaced(99L);
        verify(current).replaceBatch(11L, 99L);
        verify(uploadFileRepository).save(any());
    }
}
