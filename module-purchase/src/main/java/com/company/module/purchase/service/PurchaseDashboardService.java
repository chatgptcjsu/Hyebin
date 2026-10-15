package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import com.company.module.purchase.entity.PurchaseLine;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.repository.PurchaseLineRepository;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseDashboardService {

    private final PurchaseMonthSnapshotRepository snapshotRepository;
    private final PurchaseUploadBatchRepository batchRepository;
    private final PurchaseLineRepository lineRepository;

    public PurchaseDashboardResponse getDashboard(Integer year) {
        if (year == null || year < 2000 || year > 2200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<PurchaseMonthSnapshot> snapshots = snapshotRepository
            .findByPurchaseYearAndDeletedYnOrderByPurchaseMonthAsc(year, "N");
        Map<Integer, PurchaseUploadBatch> activeBatches = new HashMap<>();
        for (PurchaseMonthSnapshot snapshot : snapshots) {
            PurchaseUploadBatch batch = batchRepository.findById(snapshot.getCurrentUploadBatchId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
            activeBatches.put(snapshot.getPurchaseMonth(), batch);
        }

        List<PurchaseDashboardResponse.MonthPoint> months = new ArrayList<>();
        BigDecimal totalPlan = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            PurchaseUploadBatch batch = activeBatches.get(month);
            BigDecimal plan = batch == null ? BigDecimal.ZERO : batch.getPlanAmount();
            BigDecimal actual = batch == null ? BigDecimal.ZERO : batch.getActualAmount();
            totalPlan = totalPlan.add(plan);
            totalActual = totalActual.add(actual);
            months.add(PurchaseDashboardResponse.MonthPoint.builder()
                .month(month)
                .uploaded(batch != null)
                .planAmount(plan)
                .actualAmount(actual)
                .varianceAmount(actual.subtract(plan))
                .achievementRate(rate(actual, plan))
                .build());
        }

        List<Long> batchIds = activeBatches.values().stream().map(PurchaseUploadBatch::getUploadBatchId).toList();
        List<PurchaseLine> lines = batchIds.isEmpty() ? List.of()
            : lineRepository.findByUploadBatchIdInAndDeletedYn(batchIds, "N");
        Map<String, BigDecimal> byBusinessUnit = new LinkedHashMap<>();
        Map<String, BigDecimal> bySupplier = new HashMap<>();
        for (PurchaseLine line : lines) {
            BigDecimal actual = amount(line.getActualAmount());
            String businessUnit = line.getBusinessUnit() == null || line.getBusinessUnit().isBlank()
                ? "미분류" : line.getBusinessUnit();
            byBusinessUnit.merge(businessUnit, actual, BigDecimal::add);
            bySupplier.merge(line.getSupplierName(), actual, BigDecimal::add);
        }

        BigDecimal denominator = totalActual;
        List<PurchaseDashboardResponse.BreakdownItem> businessUnits = byBusinessUnit.entrySet().stream()
            .map(entry -> breakdown(entry.getKey(), entry.getValue(), denominator))
            .sorted(Comparator.comparing(PurchaseDashboardResponse.BreakdownItem::getAmount).reversed())
            .toList();
        List<PurchaseDashboardResponse.BreakdownItem> topSuppliers = bySupplier.entrySet().stream()
            .map(entry -> breakdown(entry.getKey(), entry.getValue(), denominator))
            .sorted(Comparator.comparing(PurchaseDashboardResponse.BreakdownItem::getAmount).reversed())
            .limit(10)
            .toList();

        return PurchaseDashboardResponse.builder()
            .year(year)
            .totalPlanAmount(totalPlan)
            .totalActualAmount(totalActual)
            .totalVarianceAmount(totalActual.subtract(totalPlan))
            .achievementRate(rate(totalActual, totalPlan))
            .monthly(months)
            .businessUnits(businessUnits)
            .topSuppliers(topSuppliers)
            .build();
    }

    private PurchaseDashboardResponse.BreakdownItem breakdown(String name, BigDecimal amount, BigDecimal total) {
        return PurchaseDashboardResponse.BreakdownItem.builder()
            .name(name)
            .amount(amount)
            .shareRate(rate(amount, total))
            .build();
    }

    private BigDecimal rate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
