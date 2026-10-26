package com.company.module.purchase.service;

import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.entity.PurchaseUploadFile;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import com.company.module.purchase.repository.PurchaseUploadFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseUploadHistoryServiceTest {

    @Mock private PurchaseMonthSnapshotRepository snapshotRepository;
    @Mock private PurchaseUploadBatchRepository batchRepository;
    @Mock private PurchaseUploadFileRepository uploadFileRepository;
    @InjectMocks private PurchaseUploadHistoryService historyService;

    @Test
    void historyContainsCurrentVersionAndConvertsUtcTimestampToKst() {
        LocalDateTime utcTime = LocalDateTime.of(2026, 10, 26, 3, 0, 0);
        PurchaseMonthSnapshot snapshot = org.mockito.Mockito.mock(PurchaseMonthSnapshot.class);
        PurchaseUploadBatch currentBatch = org.mockito.Mockito.mock(PurchaseUploadBatch.class);
        when(snapshotRepository.findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAsc(2026, "N"))
            .thenReturn(List.of(snapshot));
        when(snapshot.getCurrentUploadBatchId()).thenReturn(20L);
        when(batchRepository.findById(20L)).thenReturn(Optional.of(currentBatch));
        when(currentBatch.getDeletedYn()).thenReturn("N");
        when(currentBatch.getUploadBatchId()).thenReturn(20L);
        when(currentBatch.getPurchaseYear()).thenReturn(2026);
        when(currentBatch.getPurchaseMonth()).thenReturn(7);
        when(currentBatch.getOriginalFilename()).thenReturn("2026년 7월 구매실적.xlsx");
        when(currentBatch.getPlanAmount()).thenReturn(new BigDecimal("100"));
        when(currentBatch.getActualAmount()).thenReturn(new BigDecimal("95"));
        when(currentBatch.getCreatedAt()).thenReturn(utcTime);
        when(currentBatch.getLastUploadedAt()).thenReturn(utcTime);
        when(uploadFileRepository.findAvailableBatchIds(List.of(20L), "N")).thenReturn(Set.of(20L));

        var history = historyService.getHistory(2026);

        assertEquals(1, history.size());
        assertEquals(20L, history.get(0).getUploadBatchId());
        assertTrue(history.get(0).isOriginalFileAvailable());
        assertEquals(OffsetDateTime.of(2026, 10, 26, 12, 0, 0, 0, ZoneOffset.ofHours(9)),
            history.get(0).getLastUploadedAt());
    }
}
