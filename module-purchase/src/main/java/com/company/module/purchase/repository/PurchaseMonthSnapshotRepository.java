package com.company.module.purchase.repository;

import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PurchaseMonthSnapshotRepository extends JpaRepository<PurchaseMonthSnapshot, Long> {
    Optional<PurchaseMonthSnapshot> findByPurchaseYearAndPurchaseMonthAndDeletedYn(
        Integer purchaseYear, Integer purchaseMonth, String deletedYn);

    List<PurchaseMonthSnapshot> findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAsc(
        Integer purchaseYear, String deletedYn);
}
