package com.company.module.purchase.service;

import java.math.BigDecimal;

public record ParsedPurchaseLine(
    Integer sourceRowNo,
    String businessUnit,
    String importType,
    String supplierCode,
    String unitName,
    BigDecimal previousQuantity,
    BigDecimal previousAmount,
    BigDecimal planQuantity,
    BigDecimal planAmount,
    BigDecimal actualQuantity,
    BigDecimal actualAmount,
    String noteText,
    String supplierName
) {
}
