package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import com.company.module.purchase.entity.PurchaseCategory;
import com.company.module.purchase.entity.PurchaseMonthSnapshot;
import com.company.module.purchase.entity.PurchaseUploadBatch;
import com.company.module.purchase.repository.PurchaseCategoryRepository;
import com.company.module.purchase.repository.PurchaseMonthSnapshotRepository;
import com.company.module.purchase.repository.PurchaseUploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseDashboardService {

    private final PurchaseMonthSnapshotRepository snapshotRepository;
    private final PurchaseUploadBatchRepository batchRepository;
    private final PurchaseCategoryRepository categoryRepository;

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
            BigDecimal plan = batch == null ? BigDecimal.ZERO : amount(batch.getPlanAmount());
            BigDecimal actual = batch == null ? BigDecimal.ZERO : amount(batch.getActualAmount());
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
        List<PurchaseCategory> categories = batchIds.isEmpty() ? List.of()
            : categoryRepository.findByUploadBatchIdInAndDeletedYn(batchIds, "N");
        Map<Long, List<PurchaseCategory>> categoriesByBatch = categories.stream()
            .collect(Collectors.groupingBy(PurchaseCategory::getUploadBatchId));

        List<PurchaseDashboardResponse.MonthlyReason> monthlyReasons = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            PurchaseUploadBatch batch = activeBatches.get(month);
            if (batch == null) {
                monthlyReasons.add(emptyReason(month));
                continue;
            }
            List<PurchaseCategory> monthCategories = categoriesByBatch.getOrDefault(batch.getUploadBatchId(), List.of());
            if (monthCategories.isEmpty()) {
                monthlyReasons.add(emptyReason(month));
                continue;
            }
            for (PurchaseCategory category : monthCategories) {
                monthlyReasons.add(PurchaseDashboardResponse.MonthlyReason.builder()
                    .month(month)
                    .uploaded(true)
                    .categoryName(category.getCategoryName())
                    .planAmount(amount(category.getPlanAmount()))
                    .actualAmount(amount(category.getActualAmount()))
                    .planVarianceAmount(amount(category.getPlanVarianceAmount()))
                    .previousMonthVarianceAmount(amount(category.getPreviousMonthVarianceAmount()))
                    .planVarianceReason(category.getPlanVarianceReason())
                    .previousMonthVarianceReason(category.getPreviousMonthVarianceReason())
                    .build());
            }
        }

        return PurchaseDashboardResponse.builder()
            .year(year)
            .totalPlanAmount(totalPlan)
            .totalActualAmount(totalActual)
            .totalVarianceAmount(totalActual.subtract(totalPlan))
            .achievementRate(rate(totalActual, totalPlan))
            .monthly(months)
            .monthlyReasons(monthlyReasons)
            .build();
    }

    private PurchaseDashboardResponse.MonthlyReason emptyReason(int month) {
        return PurchaseDashboardResponse.MonthlyReason.builder()
            .month(month)
            .uploaded(false)
            .planAmount(BigDecimal.ZERO)
            .actualAmount(BigDecimal.ZERO)
            .planVarianceAmount(BigDecimal.ZERO)
            .previousMonthVarianceAmount(BigDecimal.ZERO)
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
