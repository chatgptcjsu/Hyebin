package com.company.module.purchase.service;

import java.math.BigDecimal;
import java.util.List;

public record ParsedPurchaseWorkbook(
    Integer purchaseYear,
    Integer purchaseMonth,
    BigDecimal planAmount,
    BigDecimal actualAmount,
    List<ParsedPurchaseCategory> categories
) {
}
