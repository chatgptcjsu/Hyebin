package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseDetailSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PurchaseDetailSheetRepository extends JpaRepository<PurchaseDetailSheet, Long> {
    Optional<PurchaseDetailSheet> findByUploadBatchIdAndDeletedYn(Long uploadBatchId, String deletedYn);
    boolean existsByUploadBatchIdAndDeletedYn(Long uploadBatchId, String deletedYn);
}
