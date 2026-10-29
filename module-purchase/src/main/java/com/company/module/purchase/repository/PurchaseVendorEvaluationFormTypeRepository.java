package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseVendorEvaluationFormType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseVendorEvaluationFormTypeRepository extends JpaRepository<PurchaseVendorEvaluationFormType, Long> {
    List<PurchaseVendorEvaluationFormType> findByDeletedYnOrderByFormTypeNameAsc(String deletedYn);

    boolean existsByFormTypeNameAndDeletedYn(String formTypeName, String deletedYn);

    Optional<PurchaseVendorEvaluationFormType> findByFormTypeNameAndDeletedYn(String formTypeName, String deletedYn);
}
