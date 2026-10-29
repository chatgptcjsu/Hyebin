package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.dto.PurchaseVendorEvaluatorOption;
import com.company.module.purchase.dto.PurchaseVendorEvaluatorRegisterRequest;
import com.company.module.purchase.entity.PurchaseVendorEvaluatorMaster;
import com.company.module.purchase.repository.PurchaseVendorEvaluatorMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVendorEvaluatorRegistryService {

    private static final String ACTIVE = "N";

    private final PurchaseVendorEvaluatorMasterRepository evaluatorRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<PurchaseVendorEvaluatorOption> getEvaluators() {
        return evaluatorRepository.findByDeletedYnOrderByEvaluatorNameAsc(ACTIVE).stream()
            .map(evaluator -> new PurchaseVendorEvaluatorOption(
                evaluator.getPurchaseVendorEvaluatorId(), evaluator.getEvaluatorName()))
            .toList();
    }

    @Transactional
    public PurchaseVendorEvaluatorOption register(PurchaseVendorEvaluatorRegisterRequest request) {
        if (request == null || request.evaluatorName() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String evaluatorName = request.evaluatorName().trim();
        if (evaluatorName.isEmpty() || evaluatorName.length() > 100
                || evaluatorRepository.existsByEvaluatorNameAndDeletedYn(evaluatorName, ACTIVE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try {
            PurchaseVendorEvaluatorMaster saved = evaluatorRepository.saveAndFlush(PurchaseVendorEvaluatorMaster.builder()
                .evaluatorName(evaluatorName)
                .createdBy(currentUserProvider.getCurrentUserId())
                .build());
            return new PurchaseVendorEvaluatorOption(saved.getPurchaseVendorEvaluatorId(), saved.getEvaluatorName());
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
