package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseVendorEvaluatorMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseVendorEvaluatorMasterRepository extends JpaRepository<PurchaseVendorEvaluatorMaster, Long> {
    List<PurchaseVendorEvaluatorMaster> findByDeletedYnOrderByEvaluatorNameAsc(String deletedYn);

    boolean existsByEvaluatorNameAndDeletedYn(String evaluatorName, String deletedYn);

    Optional<PurchaseVendorEvaluatorMaster> findByEvaluatorNameAndDeletedYn(String evaluatorName, String deletedYn);
}
