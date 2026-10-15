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
        assertEquals(1, result.categories().size());
        assertEquals("기계설비공사", result.categories().get(0).categoryName());
        assertEquals(new BigDecimal("10336.786152"), result.categories().get(0).previousActualAmount());
    }

    @Test
    void rejectsWorkbookWhenSummaryAndCategoryAmountsDoNotMatch() throws Exception {
        MockMultipartFile file = workbook("2026년 1월 구매실적", new BigDecimal("100"), new BigDecimal("50"),
            new BigDecimal("99"), new BigDecimal("49"));

        assertThrows(RuntimeException.class, () -> parser.parse(file));
    }

    private MockMultipartFile workbook(String title, BigDecimal totalPlan, BigDecimal totalActual,
                                       BigDecimal categoryPlan, BigDecimal categoryActual) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var summary = workbook.createSheet("구매실적요약");
            summary.createRow(0).createCell(0).setCellValue(title);
            var category = summary.createRow(13);
            category.createCell(0).setCellValue("기계설비공사");
            category.createCell(1).setCellValue(10336.786152);
            category.createCell(2).setCellValue(0);
            category.createCell(3).setCellValue(categoryPlan.doubleValue());
            category.createCell(4).setCellValue(categoryPlan.doubleValue());
            category.createCell(5).setCellValue(0);
            category.createCell(6).setCellValue(categoryActual.doubleValue());
            category.createCell(7).setCellValue(categoryActual.doubleValue());
            category.createCell(8).setCellValue(categoryActual.subtract(categoryPlan).doubleValue());
            category.createCell(9).setCellValue(-9654.180152);
            category.createCell(10).setCellValue("계획 대비 사유");
            category.createCell(11).setCellValue("전월 대비 사유");
            var total = summary.createRow(14);
            total.createCell(0).setCellValue("합계");
            total.createCell(4).setCellValue(totalPlan.doubleValue());
            total.createCell(7).setCellValue(totalActual.doubleValue());

            var ignoredSecondSheet = workbook.createSheet("기계설비공사");
            ignoredSecondSheet.createRow(0).createCell(0).setCellValue("이 시트는 읽으면 안 됨");

            workbook.write(output);
            return new MockMultipartFile("file", "purchase.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
