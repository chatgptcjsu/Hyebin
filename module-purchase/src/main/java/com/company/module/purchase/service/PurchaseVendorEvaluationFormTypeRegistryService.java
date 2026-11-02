package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.dto.PurchaseVendorEvaluationFormTypeOption;
import com.company.module.purchase.entity.PurchaseVendorEvaluationFormType;
import com.company.module.purchase.entity.PurchaseVendorEvaluationFormTypeFile;
import com.company.module.purchase.repository.PurchaseVendorEvaluationFormTypeFileRepository;
import com.company.module.purchase.repository.PurchaseVendorEvaluationFormTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVendorEvaluationFormTypeRegistryService {

    private static final String ACTIVE = "N";

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final PurchaseVendorEvaluationFormTypeRepository formTypeRepository;
    private final PurchaseVendorEvaluationFormTypeFileRepository formTypeFileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final Map<Long, Boolean> attachmentStatusCache = new ConcurrentHashMap<>();

    public List<PurchaseVendorEvaluationFormTypeOption> getFormTypes() {
        return formTypeRepository.findByDeletedYnOrderByFormTypeNameAsc(ACTIVE).stream()
            .map(this::toOption)
            .filter(PurchaseVendorEvaluationFormTypeOption::hasFile)
            .toList();
    }

    @Transactional
    public PurchaseVendorEvaluationFormTypeOption register(String requestedName, MultipartFile file) {
        String formTypeName = requestedName == null ? "" : requestedName.trim();
        if (formTypeName.isEmpty() || formTypeName.length() > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        ValidatedFile validatedFile = validateFile(file);
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            PurchaseVendorEvaluationFormType existing = formTypeRepository.findByFormTypeName(formTypeName).orElse(null);
            if (existing != null && existing.getOriginalFilename() != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            PurchaseVendorEvaluationFormType saved;
            if (existing != null) {
                existing.attachFile(validatedFile.filename(), validatedFile.contentType(), userId);
                saved = formTypeRepository.saveAndFlush(existing);
            } else {
                saved = formTypeRepository.saveAndFlush(PurchaseVendorEvaluationFormType.builder()
                    .formTypeName(formTypeName)
                    .originalFilename(validatedFile.filename())
                    .fileContentType(validatedFile.contentType())
                    .createdBy(userId)
                    .build());
            }
            formTypeFileRepository.save(PurchaseVendorEvaluationFormTypeFile.builder()
                .purchaseVendorEvaluationFormTypeId(saved.getPurchaseVendorEvaluationFormTypeId())
                .fileData(validatedFile.bytes())
                .createdBy(userId)
                .build());
            attachmentStatusCache.put(saved.getPurchaseVendorEvaluationFormTypeId(), true);
            return toOption(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    public DownloadedFormFile getFile(Long formTypeId) {
        PurchaseVendorEvaluationFormType formType = formTypeRepository
            .findByPurchaseVendorEvaluationFormTypeIdAndDeletedYn(formTypeId, ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        if (formType.getOriginalFilename() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PurchaseVendorEvaluationFormTypeFile file = formTypeFileRepository
            .findByPurchaseVendorEvaluationFormTypeId(formTypeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        attachmentStatusCache.put(formTypeId, true);
        return new DownloadedFormFile(formType.getOriginalFilename(), formType.getFileContentType(), file.getFileData());
    }

    private PurchaseVendorEvaluationFormTypeOption toOption(PurchaseVendorEvaluationFormType formType) {
        Long formTypeId = formType.getPurchaseVendorEvaluationFormTypeId();
        boolean hasFile = formType.getOriginalFilename() != null && attachmentStatusCache.computeIfAbsent(
            formTypeId, id -> formTypeFileRepository.findByPurchaseVendorEvaluationFormTypeId(id).isPresent());
        return new PurchaseVendorEvaluationFormTypeOption(formTypeId, formType.getFormTypeName(),
            formType.getOriginalFilename(), formType.getFileContentType(), hasFile ? "/purchase-api/vendor-evaluation-form-types/"
            + formTypeId + "/file" : null, hasFile);
    }

    public boolean isRegisteredAndAttached(String formTypeName) {
        return formTypeRepository.findByFormTypeNameAndDeletedYn(formTypeName, ACTIVE)
            .filter(formType -> formType.getOriginalFilename() != null)
            .map(formType -> attachmentStatusCache.computeIfAbsent(formType.getPurchaseVendorEvaluationFormTypeId(),
                id -> formTypeFileRepository.findByPurchaseVendorEvaluationFormTypeId(id).isPresent()))
            .orElse(false);
    }

    private ValidatedFile validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String filename = original.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (filename.isEmpty() || filename.length() > 255 || filename.startsWith(".")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String contentType;
        if ("pdf".equals(extension) && startsWith(bytes, new byte[]{'%', 'P', 'D', 'F', '-'})) {
            contentType = "application/pdf";
        } else if (("xlsx".equals(extension) || "xls".equals(extension))
                && isValidExcelWorkbook(bytes)) {
            contentType = "xlsx".equals(extension)
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/vnd.ms-excel";
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new ValidatedFile(filename, contentType, bytes);
    }

    private boolean isValidExcelWorkbook(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            return workbook.getNumberOfSheets() > 0;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private boolean startsWith(byte[] bytes, byte[] signature) {
        if (bytes.length < signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) return false;
        }
        return true;
    }

    public record DownloadedFormFile(String filename, String contentType, byte[] bytes) { }

    private record ValidatedFile(String filename, String contentType, byte[] bytes) { }
}
