package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.dto.PurchaseVendorEvaluationFormTypeOption;
import com.company.module.purchase.dto.PurchaseVendorEvaluationFormTypeRequest;
import com.company.module.purchase.entity.PurchaseVendorEvaluationFormType;
import com.company.module.purchase.repository.PurchaseVendorEvaluationFormTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVendorEvaluationFormTypeRegistryService {

    private static final String ACTIVE = "N";

    private final PurchaseVendorEvaluationFormTypeRepository formTypeRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<PurchaseVendorEvaluationFormTypeOption> getFormTypes() {
        return formTypeRepository.findByDeletedYnOrderByFormTypeNameAsc(ACTIVE).stream()
            .map(formType -> new PurchaseVendorEvaluationFormTypeOption(
                formType.getPurchaseVendorEvaluationFormTypeId(), formType.getFormTypeName()))
            .toList();
    }

    @Transactional
    public PurchaseVendorEvaluationFormTypeOption register(PurchaseVendorEvaluationFormTypeRequest request) {
        if (request == null || request.formTypeName() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String formTypeName = request.formTypeName().trim();
        if (formTypeName.isEmpty() || formTypeName.length() > 100
                || formTypeRepository.existsByFormTypeNameAndDeletedYn(formTypeName, ACTIVE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try {
            PurchaseVendorEvaluationFormType saved = formTypeRepository.saveAndFlush(PurchaseVendorEvaluationFormType.builder()
                .formTypeName(formTypeName)
                .createdBy(currentUserProvider.getCurrentUserId())
                .build());
            return new PurchaseVendorEvaluationFormTypeOption(
                saved.getPurchaseVendorEvaluationFormTypeId(), saved.getFormTypeName());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
