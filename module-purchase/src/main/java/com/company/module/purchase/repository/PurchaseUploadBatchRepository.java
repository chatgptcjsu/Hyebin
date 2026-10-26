package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseUploadBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseUploadBatchRepository extends JpaRepository<PurchaseUploadBatch, Long> {
    boolean existsByPurchaseYearAndPurchaseMonthAndFileSha256AndDeletedYn(
        Integer purchaseYear, Integer purchaseMonth, String fileSha256, String deletedYn);

    List<PurchaseUploadBatch> findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAscCreatedAtDesc(
        Integer purchaseYear, String deletedYn);

}
