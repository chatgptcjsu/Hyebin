package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseUploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PurchaseUploadFileRepository extends JpaRepository<PurchaseUploadFile, Long> {
    @Query("select file.uploadBatchId from PurchaseUploadFile file "
        + "where file.uploadBatchId in :batchIds and file.deletedYn = :deletedYn")
    Set<Long> findAvailableBatchIds(@Param("batchIds") Collection<Long> batchIds,
                                    @Param("deletedYn") String deletedYn);

    Optional<PurchaseUploadFile> findByUploadBatchIdAndDeletedYn(Long uploadBatchId, String deletedYn);
    boolean existsByUploadBatchIdAndDeletedYn(Long uploadBatchId, String deletedYn);
}
