package com.company.module.purchase.dto;

import java.util.List;

public record PurchaseDetailSheetResponse(
    boolean available,
    String message,
    Integer purchaseYear,
    Integer purchaseMonth,
    String sheetName,
    String title,
    String sectionTitle,
    String unitLabel,
    String previousMonthLabel,
    String planMonthLabel,
    String actualMonthLabel,
    List<List<String>> rows,
    List<List<String>> summaryRows,
    List<String> mainIssues,
    List<String> futureImprovements
) {
}
