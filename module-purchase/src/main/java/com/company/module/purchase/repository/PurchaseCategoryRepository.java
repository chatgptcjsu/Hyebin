package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PurchaseCategoryRepository extends JpaRepository<PurchaseCategory, Long> {
    List<PurchaseCategory> findByUploadBatchIdInAndDeletedYn(Collection<Long> uploadBatchIds, String deletedYn);

}
