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
    private List<MonthlyReason> monthlyReasons;

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
    public static class MonthlyReason {
        private Integer month;
        private boolean uploaded;
        private String categoryName;
        private BigDecimal planAmount;
        private BigDecimal actualAmount;
        private BigDecimal planVarianceAmount;
        private BigDecimal previousMonthVarianceAmount;
        private String planVarianceReason;
        private String previousMonthVarianceReason;
    }
}
