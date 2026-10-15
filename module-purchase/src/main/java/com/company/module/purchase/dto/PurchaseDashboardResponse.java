package com.company.module.purchase.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PurchaseDashboardResponse {
    private Integer year;
    private BigDecimal totalPlanAmount;
    private BigDecimal totalActualAmount;
    private BigDecimal totalVarianceAmount;
    private BigDecimal achievementRate;
    private List<MonthPoint> monthly;
    private List<BreakdownItem> businessUnits;
    private List<BreakdownItem> topSuppliers;

    @Getter
    @Builder
    public static class MonthPoint {
        private Integer month;
        private boolean uploaded;
        private BigDecimal planAmount;
        private BigDecimal actualAmount;
        private BigDecimal varianceAmount;
        private BigDecimal achievementRate;
    }

    @Getter
    @Builder
    public static class BreakdownItem {
        private String name;
        private BigDecimal amount;
        private BigDecimal shareRate;
    }
}
