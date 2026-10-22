package com.company.module.purchase.service;

import java.util.List;

public record ParsedPurchaseDetailSheet(
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
