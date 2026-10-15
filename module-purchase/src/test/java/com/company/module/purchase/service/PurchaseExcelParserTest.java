package com.company.module.purchase.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseExcelParserTest {

    private final PurchaseExcelParser parser = new PurchaseExcelParser();

    @Test
    void parsesMonthlyPurchaseWorkbookAndMatchesSummaryTotals() throws Exception {
        MockMultipartFile file = workbook("'26년 1월 구매실적", new BigDecimal("897.7069"), new BigDecimal("682.606"),
            new BigDecimal("897.7069"), new BigDecimal("682.606"));

        ParsedPurchaseWorkbook result = parser.parse(file);

        assertEquals(2026, result.purchaseYear());
        assertEquals(1, result.purchaseMonth());
        assertEquals(new BigDecimal("897.7069"), result.planAmount());
        assertEquals(new BigDecimal("682.606"), result.actualAmount());
        assertEquals(1, result.lines().size());
        assertEquals("청수", result.lines().get(0).supplierName());
        assertEquals("공통", result.lines().get(0).businessUnit());
    }

    @Test
    void rejectsWorkbookWhenSummaryAndDetailAmountsDoNotMatch() throws Exception {
        MockMultipartFile file = workbook("2026년 1월 구매실적", new BigDecimal("100"), new BigDecimal("50"),
            new BigDecimal("99"), new BigDecimal("49"));

        assertThrows(RuntimeException.class, () -> parser.parse(file));
    }

    private MockMultipartFile workbook(String title, BigDecimal plan, BigDecimal actual,
                                       BigDecimal detailPlan, BigDecimal detailActual) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var summary = workbook.createSheet("구매실적요약");
            summary.createRow(0).createCell(0).setCellValue(title);
            var summaryTotal = summary.createRow(13);
            summaryTotal.createCell(0).setCellValue("기계설비공사");
            summaryTotal.createCell(4).setCellValue(plan.doubleValue());
            summaryTotal.createCell(7).setCellValue(actual.doubleValue());

            var detail = workbook.createSheet("기계설비공사");
            var line = detail.createRow(4);
            line.createCell(1).setCellValue("기계설비공사");
            line.createCell(2).setCellValue("공통");
            line.createCell(3).setCellValue("내자");
            line.createCell(5).setCellValue("SIK");
            line.createCell(9).setCellValue(detailPlan.doubleValue());
            line.createCell(11).setCellValue(detailActual.doubleValue());
            line.createCell(12).setCellValue("테스트 구매 항목");
            line.createCell(13).setCellValue("청수");

            workbook.write(output);
            return new MockMultipartFile("file", "purchase.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
