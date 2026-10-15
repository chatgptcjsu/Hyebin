package com.company.module.purchase.service;

import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.dto.PurchaseHistoryResponse;
import com.company.module.purchase.dto.PurchaseLineResponse;
import com.company.module.purchase.dto.PurchaseUploadResponse;
import com.company.module.purchase.entity.PurchaseLine;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.repository.PurchaseLineRepository;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseUploadService {

    private final PurchaseExcelParser excelParser;
    private final PurchaseUploadBatchRepository batchRepository;
    private final PurchaseMonthSnapshotRepository snapshotRepository;
    private final PurchaseLineRepository lineRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public PurchaseUploadResponse upload(MultipartFile file, boolean confirmReplace) {
        ParsedPurchaseWorkbook parsed = excelParser.parse(file);
        String sha256 = sha256(file);
        PurchaseMonthSnapshot current = snapshotRepository
            .findByPurchaseYearAndPurchaseMonthAndDeletedYn(parsed.purchaseYear(), parsed.purchaseMonth(), "N")
            .orElse(null);

        if (current != null) {
            PurchaseUploadBatch active = batchRepository.findById(current.getCurrentUploadBatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
            if (active.getFileSha256().equals(sha256)) {
                return toResponse(active, true, false);
            }
            if (!confirmReplace) {
                return PurchaseUploadResponse.builder()
                    .purchaseYear(parsed.purchaseYear())
                    .purchaseMonth(parsed.purchaseMonth())
                    .originalFilename(safeFilename(file.getOriginalFilename()))
                    .uploadStatus("PENDING_REPLACEMENT_CONFIRMATION")
                    .planAmount(parsed.planAmount())
                    .actualAmount(parsed.actualAmount())
                    .detailRowCount(parsed.lines().size())
                    .alreadyCurrent(false)
                    .requiresReplacementConfirmation(true)
                    .build();
            }
        }

        Long userId = currentUserProvider.getCurrentUserId();
        PurchaseUploadBatch batch = batchRepository.save(PurchaseUploadBatch.builder()
            .purchaseYear(parsed.purchaseYear())
            .purchaseMonth(parsed.purchaseMonth())
            .originalFilename(safeFilename(file.getOriginalFilename()))
            .fileSha256(sha256)
            .planAmount(parsed.planAmount())
            .actualAmount(parsed.actualAmount())
            .detailRowCount(parsed.lines().size())
            .createdBy(userId)
            .build());

        List<PurchaseLine> lines = parsed.lines().stream()
            .map(line -> PurchaseLine.builder()
                .uploadBatchId(batch.getUploadBatchId())
                .sourceRowNo(line.sourceRowNo())
                .businessUnit(line.businessUnit())
                .importType(line.importType())
                .supplierCode(line.supplierCode())
                .unitName(line.unitName())
                .previousQuantity(line.previousQuantity())
                .previousAmount(line.previousAmount())
                .planQuantity(line.planQuantity())
                .planAmount(line.planAmount())
                .actualQuantity(line.actualQuantity())
                .actualAmount(line.actualAmount())
                .noteText(line.noteText())
                .supplierName(line.supplierName())
                .createdBy(userId)
                .build())
            .toList();
        lineRepository.saveAll(lines);

        if (current == null) {
            snapshotRepository.save(PurchaseMonthSnapshot.builder()
                .purchaseYear(parsed.purchaseYear())
                .purchaseMonth(parsed.purchaseMonth())
                .currentUploadBatchId(batch.getUploadBatchId())
                .createdBy(userId)
                .build());
        } else {
            batchRepository.findById(current.getCurrentUploadBatchId())
                .ifPresent(previous -> previous.markReplaced(userId));
            current.replaceBatch(batch.getUploadBatchId(), userId);
        }
        return toResponse(batch, false, false);
    }

    public List<PurchaseHistoryResponse> history(Integer year) {
        return batchRepository.findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAscCreatedAtDesc(year, "N")
            .stream().map(PurchaseHistoryResponse::from).toList();
    }

    public Page<PurchaseLineResponse> detail(Integer year, Integer month, String businessUnit,
                                             String supplierName, int page, int size) {
        if (page < 0 || size < 1 || size > 200 || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PurchaseMonthSnapshot snapshot = snapshotRepository
            .findByPurchaseYearAndPurchaseMonthAndDeletedYn(year, month, "N")
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        Page<PurchaseLineResponse> result = lineRepository.searchCurrentBatch(
            snapshot.getCurrentUploadBatchId(), emptyToNull(businessUnit), emptyToNull(supplierName),
            PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sourceRowNo")))
            .map(PurchaseLineResponse::from);
        return result;
    }

    private PurchaseUploadResponse toResponse(PurchaseUploadBatch batch, boolean alreadyCurrent,
                                              boolean requiresConfirmation) {
        return PurchaseUploadResponse.builder()
            .uploadBatchId(batch.getUploadBatchId())
            .purchaseYear(batch.getPurchaseYear())
            .purchaseMonth(batch.getPurchaseMonth())
            .originalFilename(batch.getOriginalFilename())
            .uploadStatus(batch.getUploadStatus())
            .planAmount(batch.getPlanAmount())
            .actualAmount(batch.getActualAmount())
            .detailRowCount(batch.getDetailRowCount())
            .alreadyCurrent(alreadyCurrent)
            .requiresReplacementConfirmation(requiresConfirmation)
            .build();
    }

    private String sha256(MultipartFile file) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getBytes()));
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "purchase-upload.xlsx";
        String clean = filename.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
