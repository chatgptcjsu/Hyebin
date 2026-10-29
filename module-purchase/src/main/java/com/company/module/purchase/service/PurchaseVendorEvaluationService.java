package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.core.security.CurrentUserProvider;
import com.company.module.purchase.dto.PurchaseVendorEvaluationRequest;
import com.company.module.purchase.dto.PurchaseVendorEvaluationResponse;
import com.company.module.purchase.entity.PurchaseVendorEvaluation;
import com.company.module.purchase.entity.PurchaseVendorEvaluatorMaster;
import com.company.module.purchase.repository.PurchaseVendorEvaluationRepository;
import com.company.module.purchase.repository.PurchaseVendorEvaluatorMasterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVendorEvaluationService {

    private static final String ACTIVE = "N";
    private static final Set<String> SUPPORTED_EVALUATION_TYPES = Set.of("MECHANICAL", "MAINTENANCE");

    private final PurchaseVendorEvaluationRepository evaluationRepository;
    private final PurchaseVendorEvaluatorMasterRepository evaluatorMasterRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;

    public List<PurchaseVendorEvaluationScoring.Criterion> getCriteria() {
        return PurchaseVendorEvaluationScoring.criteria();
    }

    public List<PurchaseVendorEvaluationResponse> getEvaluations(Integer year, String evaluationType) {
        validateYear(year);
        String type = validateEvaluationType(evaluationType);
        return evaluationRepository
            .findByEvaluationYearAndEvaluationTypeAndDeletedYnOrderByEvaluationDateDescCreatedAtDesc(year, type, ACTIVE)
            .stream().map(this::toResponse).toList();
    }

    public PurchaseVendorEvaluationResponse getEvaluation(Long id) {
        return evaluationRepository.findByPurchaseVendorEvaluationIdAndDeletedYn(id, ACTIVE)
            .map(this::toResponse)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Transactional
    public PurchaseVendorEvaluationResponse create(PurchaseVendorEvaluationRequest request) {
        ValidatedEvaluation validated = validate(request);
        if (evaluationRepository.existsByEvaluationYearAndEvaluationTypeAndSupplierNameAndDeletedYn(
                validated.year(), validated.evaluationType(), validated.supplierName(), ACTIVE)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long userId = currentUserProvider.getCurrentUserId();
        try {
            PurchaseVendorEvaluation saved = evaluationRepository.save(PurchaseVendorEvaluation.builder()
                .evaluationYear(validated.year())
                .evaluationType(validated.evaluationType())
                .supplierName(validated.supplierName())
                .evaluatorName(validated.evaluatorName())
                .evaluationDate(request.evaluationDate())
                .answersJson(writeJson(request.answers()))
                .scoresJson(writeJson(validated.calculation().criteria()))
                .totalScore(validated.calculation().totalScore())
                .finalGrade(validated.calculation().grade())
                .createdBy(userId)
                .build());
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Transactional
    public PurchaseVendorEvaluationResponse update(Long id, PurchaseVendorEvaluationRequest request) {
        ValidatedEvaluation validated = validate(request);
        PurchaseVendorEvaluation evaluation = evaluationRepository
            .findByPurchaseVendorEvaluationIdAndDeletedYn(id, ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        boolean nameTaken = evaluationRepository.existsByEvaluationYearAndEvaluationTypeAndSupplierNameAndDeletedYn(
            validated.year(), validated.evaluationType(), validated.supplierName(), ACTIVE);
        if (nameTaken && (!evaluation.getEvaluationYear().equals(validated.year())
                || !evaluation.getEvaluationType().equals(validated.evaluationType())
                || !evaluation.getSupplierName().equalsIgnoreCase(validated.supplierName()))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        evaluation.updateEvaluation(validated.evaluationType(), validated.supplierName(), validated.evaluatorName(),
            request.evaluationDate(),
            writeJson(request.answers()), writeJson(validated.calculation().criteria()),
            validated.calculation().totalScore(), validated.calculation().grade(), currentUserProvider.getCurrentUserId());
        return toResponse(evaluation);
    }

    private ValidatedEvaluation validate(PurchaseVendorEvaluationRequest request) {
        if (request == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        validateYear(request.evaluationYear());
        String evaluationType = validateEvaluationType(request.evaluationType());
        String supplierName = request.supplierName() == null ? "" : request.supplierName().trim();
        String evaluatorName = request.evaluatorName() == null ? "" : request.evaluatorName().trim();
        if (supplierName.isEmpty() || supplierName.length() > 200 || evaluatorName.isEmpty() || evaluatorName.length() > 100
                || request.evaluationDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PurchaseVendorEvaluatorMaster registeredEvaluator = evaluatorMasterRepository
            .findByEvaluatorNameAndDeletedYn(evaluatorName, ACTIVE)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        try {
            return new ValidatedEvaluation(request.evaluationYear(), evaluationType, supplierName,
                registeredEvaluator.getEvaluatorName(), PurchaseVendorEvaluationScoring.calculate(request.answers()));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 2000 || year > 2200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String validateEvaluationType(String evaluationType) {
        String type = evaluationType == null ? "" : evaluationType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_EVALUATION_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return type;
    }

    private PurchaseVendorEvaluationResponse toResponse(PurchaseVendorEvaluation evaluation) {
        try {
            Map<String, String> answers = objectMapper.readValue(evaluation.getAnswersJson(),
                new TypeReference<Map<String, String>>() { });
            List<PurchaseVendorEvaluationScoring.ScoredCriterion> scores = objectMapper.readValue(
                evaluation.getScoresJson(), new TypeReference<List<PurchaseVendorEvaluationScoring.ScoredCriterion>>() { });
            return PurchaseVendorEvaluationResponse.builder()
                .purchaseVendorEvaluationId(evaluation.getPurchaseVendorEvaluationId())
                .evaluationYear(evaluation.getEvaluationYear())
                .evaluationType(evaluation.getEvaluationType())
                .supplierName(evaluation.getSupplierName())
                .evaluatorName(evaluation.getEvaluatorName())
                .evaluationDate(evaluation.getEvaluationDate())
                .answers(answers)
                .criteriaScores(scores)
                .totalScore(evaluation.getTotalScore())
                .finalGrade(evaluation.getFinalGrade())
                .build();
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private record ValidatedEvaluation(Integer year, String evaluationType, String supplierName, String evaluatorName,
                                       PurchaseVendorEvaluationScoring.Calculation calculation) { }
}
