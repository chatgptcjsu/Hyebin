package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseUploadHistoryResponse;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadFile;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import com.company.module.purchase.repository.PurchaseUploadFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseUploadHistoryService {

    private final PurchaseMonthSnapshotRepository snapshotRepository;
    private final PurchaseUploadBatchRepository batchRepository;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Seoul");

    private final PurchaseUploadFileRepository uploadFileRepository;

    public List<PurchaseUploadHistoryResponse> getHistory(Integer year) {
        if (year == null || year < 2000 || year > 2200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<PurchaseMonthSnapshot> snapshots = snapshotRepository
            .findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAsc(year, "N");
        List<PurchaseUploadBatch> batches = snapshots.stream()
            .map(snapshot -> batchRepository.findById(snapshot.getCurrentUploadBatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE)))
            .filter(batch -> "N".equals(batch.getDeletedYn()))
            .toList();
        List<Long> batchIds = batches.stream().map(PurchaseUploadBatch::getUploadBatchId).toList();
        Set<Long> availableFileBatchIds = batchIds.isEmpty() ? Set.of()
            : uploadFileRepository.findAvailableBatchIds(batchIds, "N");
        return batches.stream()
            .map(batch -> PurchaseUploadHistoryResponse.builder()
                .uploadBatchId(batch.getUploadBatchId())
                .purchaseYear(batch.getPurchaseYear())
                .purchaseMonth(batch.getPurchaseMonth())
                .originalFilename(batch.getOriginalFilename())
                .originalFileAvailable(availableFileBatchIds.contains(batch.getUploadBatchId()))
                .planAmount(batch.getPlanAmount())
                .actualAmount(batch.getActualAmount())
                .createdAt(toDisplayTime(batch.getCreatedAt()))
                .lastUploadedAt(toDisplayTime(batch.getLastUploadedAt()))
                .build())
            .toList();
    }

    private OffsetDateTime toDisplayTime(LocalDateTime utcTime) {
        return utcTime == null ? null
            : utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(DISPLAY_ZONE).toOffsetDateTime();
    }

    public Optional<DownloadedFile> getOriginalFile(Long uploadBatchId) {
        Optional<PurchaseUploadBatch> batch = batchRepository.findById(uploadBatchId)
            .filter(item -> "N".equals(item.getDeletedYn()));
        if (batch.isEmpty()) return Optional.empty();
        Optional<PurchaseUploadFile> file = uploadFileRepository
            .findByUploadBatchIdAndDeletedYn(uploadBatchId, "N");
        return file.map(item -> new DownloadedFile(batch.get().getOriginalFilename(), item.getFileData()));
    }

    public record DownloadedFile(String filename, byte[] bytes) { }
}
