package com.company.module.purchase.service;

import java.math.BigDecimal;

public record ParsedPurchaseCategory(
    Integer sourceRowNo,
    String categoryName,
    BigDecimal previousActualAmount,
    BigDecimal planForeignAmount,
    BigDecimal planDomesticAmount,
    BigDecimal planAmount,
    BigDecimal actualForeignAmount,
    BigDecimal actualDomesticAmount,
    BigDecimal actualAmount,
    BigDecimal planVarianceAmount,
    BigDecimal previousMonthVarianceAmount,
    String planVarianceReason,
    String previousMonthVarianceReason
) {
}
