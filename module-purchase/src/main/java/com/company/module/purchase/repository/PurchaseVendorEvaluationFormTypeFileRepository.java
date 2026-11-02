package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseVendorEvaluationFormTypeFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseVendorEvaluationFormTypeFileRepository
        extends JpaRepository<PurchaseVendorEvaluationFormTypeFile, Long> {
    Optional<PurchaseVendorEvaluationFormTypeFile> findByPurchaseVendorEvaluationFormTypeId(
        Long purchaseVendorEvaluationFormTypeId);
}
