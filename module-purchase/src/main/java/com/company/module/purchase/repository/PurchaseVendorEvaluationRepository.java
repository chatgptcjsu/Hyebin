package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseVendorEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseVendorEvaluationRepository extends JpaRepository<PurchaseVendorEvaluation, Long> {
    List<PurchaseVendorEvaluation> findByEvaluationYearAndDeletedYnOrderByEvaluationDateDescCreatedAtDesc(
        Integer evaluationYear, String deletedYn);

    Optional<PurchaseVendorEvaluation> findByPurchaseVendorEvaluationIdAndDeletedYn(Long id, String deletedYn);

    boolean existsByEvaluationYearAndSupplierNameAndDeletedYn(Integer evaluationYear, String supplierName,
                                                              String deletedYn);
}
