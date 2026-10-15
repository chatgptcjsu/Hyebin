package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PurchaseLineRepository extends JpaRepository<PurchaseLine, Long> {
    List<PurchaseLine> findByUploadBatchIdInAndDeletedYn(Collection<Long> uploadBatchIds, String deletedYn);

    List<PurchaseLine> findByUploadBatchIdAndDeletedYnOrderBySourceRowNoAsc(Long uploadBatchId, String deletedYn);

    @Query("""
        select line from PurchaseLine line
        where line.uploadBatchId = :batchId
          and line.deletedYn = 'N'
          and (:businessUnit is null or line.businessUnit = :businessUnit)
          and (:supplierName is null or lower(line.supplierName) like lower(concat('%', :supplierName, '%')))
        order by line.sourceRowNo asc
        """)
    Page<PurchaseLine> searchCurrentBatch(
        @Param("batchId") Long batchId,
        @Param("businessUnit") String businessUnit,
        @Param("supplierName") String supplierName,
        Pageable pageable);
}
