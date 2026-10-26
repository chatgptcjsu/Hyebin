package com.company.module.purchase.service;

import com.company.module.purchase.dto.PurchaseDashboardResponse;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseSummaryExcelServiceTest {

    @Mock
    private PurchaseDashboardService dashboardService;

    @InjectMocks
    private PurchaseSummaryExcelService summaryExcelService;

    @Test
    void exportsMonthlySummaryWithVarianceColumnsAndAnnualTotals() throws Exception {
        List<PurchaseDashboardResponse.MonthPoint> monthly = IntStream.rangeClosed(1, 12)
            .mapToObj(month -> PurchaseDashboardResponse.MonthPoint.builder()
                .month(month)
                .uploaded(month == 1)
                .planAmount(month == 1 ? new BigDecimal("1200.4") : BigDecimal.ZERO)
                .actualAmount(month == 1 ? new BigDecimal("1350.6") : BigDecimal.ZERO)
                .varianceAmount(month == 1 ? new BigDecimal("150.2") : BigDecimal.ZERO)
                .previousMonthVarianceAmount(month == 1 ? new BigDecimal("80.8") : BigDecimal.ZERO)
                .build())
            .toList();
        when(dashboardService.getDashboard(2026)).thenReturn(PurchaseDashboardResponse.builder()
            .year(2026)
            .totalPlanAmount(new BigDecimal("1200.4"))
            .totalActualAmount(new BigDecimal("1350.6"))
            .totalVarianceAmount(new BigDecimal("150.2"))
            .totalPreviousMonthVarianceAmount(new BigDecimal("80.8"))
            .monthly(monthly)
            .build());

        byte[] bytes = summaryExcelService.export(2026);
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("2026년 월별 구매실적 요약", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("계획대비차이", sheet.getRow(3).getCell(3).getStringCellValue());
            assertEquals("전월대비차이", sheet.getRow(3).getCell(4).getStringCellValue());
            assertEquals(1200.4, sheet.getRow(4).getCell(1).getNumericCellValue(), 0.0001);
            assertEquals(80.8, sheet.getRow(4).getCell(4).getNumericCellValue(), 0.0001);
            assertEquals("누계", sheet.getRow(16).getCell(0).getStringCellValue());
            assertEquals(1350.6, sheet.getRow(16).getCell(2).getNumericCellValue(), 0.0001);
        }
    }
}
