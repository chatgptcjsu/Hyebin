package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PurchaseCategoryRepository extends JpaRepository<PurchaseCategory, Long> {
    List<PurchaseCategory> findByUploadBatchIdInAndDeletedYn(Collection<Long> uploadBatchIds, String deletedYn);

    @Query("""
        select category from PurchaseCategory category
        where category.uploadBatchId = :batchId
          and category.deletedYn = 'N'
          and (:categoryName is null or lower(category.categoryName) like lower(concat('%', :categoryName, '%')))
        order by category.sourceRowNo asc
        """)
    Page<PurchaseCategory> searchCurrentBatch(
        @Param("batchId") Long batchId,
        @Param("categoryName") String categoryName,
        Pageable pageable);
}
