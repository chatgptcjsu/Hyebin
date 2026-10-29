package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseVendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseVendorEvaluationRepository extends JpaRepository<PurchaseVendorEvaluation, Long> {
    List<PurchaseVendorEvaluation> findByEvaluationYearAndEvaluationTypeAndDeletedYnOrderByEvaluationDateDescCreatedAtDesc(
        Integer evaluationYear, String evaluationType, String deletedYn);

    List<PurchaseVendorEvaluation> findByPurchaseVendorEvaluationIdInAndEvaluationYearAndEvaluationTypeAndDeletedYn(
        List<Long> ids, Integer evaluationYear, String evaluationType, String deletedYn);

    Optional<PurchaseVendorEvaluation> findByPurchaseVendorEvaluationIdAndDeletedYn(Long id, String deletedYn);

    boolean existsByEvaluationYearAndEvaluationTypeAndSupplierNameAndDeletedYn(
        Integer evaluationYear, String evaluationType, String supplierName, String deletedYn);
}
