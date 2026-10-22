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
    void parsesSummaryAndSecondSheetDetailsFromPurchaseWorkbook() throws Exception {
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
        assertEquals("기계설비공사", result.detailSheet().sheetName());
        assertEquals("12월 실적", result.detailSheet().previousMonthLabel());
        assertEquals(1, result.detailSheet().rows().size());
        assertEquals("협력업체 예시", result.detailSheet().rows().get(0).get(12));
        assertEquals("비고 예시", result.detailSheet().rows().get(0).get(11));
        assertEquals(3, result.detailSheet().summaryRows().size());
        assertEquals("① 주요 이슈 예시", result.detailSheet().mainIssues().get(0));
        assertEquals("- 개선 예시", result.detailSheet().futureImprovements().get(0));
    }

    @Test
    void takesYearAndMonthFromFilenameWhenItContainsReportingPeriod() throws Exception {
        MockMultipartFile file = workbook("2026년 9월 구매실적자료.xlsx", "2026년 1월 구매실적",
            new BigDecimal("897.7069"), new BigDecimal("682.606"),
            new BigDecimal("897.7069"), new BigDecimal("682.606"));

        ParsedPurchaseWorkbook result = parser.parse(file);

        assertEquals(2026, result.purchaseYear());
        assertEquals(9, result.purchaseMonth());
    }

    @Test
    void rejectsWorkbookWhenSummaryAndCategoryAmountsDoNotMatch() throws Exception {
        MockMultipartFile file = workbook("2026년 1월 구매실적", new BigDecimal("100"), new BigDecimal("50"),
            new BigDecimal("99"), new BigDecimal("49"));

        assertThrows(RuntimeException.class, () -> parser.parse(file));
    }

    private MockMultipartFile workbook(String title, BigDecimal totalPlan, BigDecimal totalActual,
                                       BigDecimal categoryPlan, BigDecimal categoryActual) throws Exception {
        return workbook("purchase.xlsx", title, totalPlan, totalActual, categoryPlan, categoryActual);
    }

    private MockMultipartFile workbook(String filename, String title, BigDecimal totalPlan, BigDecimal totalActual,
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

            var detailSheet = workbook.createSheet("기계설비공사");
            detailSheet.createRow(0).createCell(1).setCellValue("26년 1월 기계설비공사 구매실적");
            var detailTitle = detailSheet.createRow(1);
            detailTitle.createCell(1).setCellValue("1) 업체별 구매실적(기계설비공사)");
            detailTitle.createCell(14).setCellValue("[단위: 백만원, VAT 별도]");
            var groupedHeader = detailSheet.createRow(2);
            groupedHeader.createCell(6).setCellValue("12월 실적");
            groupedHeader.createCell(8).setCellValue("1월 계획");
            groupedHeader.createCell(10).setCellValue("1월 실적");
            var subHeader = detailSheet.createRow(3);
            subHeader.createCell(1).setCellValue("공급품목");
            subHeader.createCell(6).setCellValue("수량");
            subHeader.createCell(7).setCellValue("금액");
            var detail = detailSheet.createRow(4);
            detail.createCell(1).setCellValue("기계설비공사");
            detail.createCell(2).setCellValue("공통");
            detail.createCell(3).setCellValue("내자");
            detail.createCell(5).setCellValue("SIK");
            detail.createCell(6).setCellValue(1);
            detail.createCell(7).setCellValue(100);
            detail.createCell(8).setCellValue(1);
            detail.createCell(9).setCellValue(90);
            detail.createCell(10).setCellValue(0);
            detail.createCell(11).setCellValue(0);
            detail.createCell(12).setCellValue("비고 예시");
            detail.createCell(13).setCellValue("협력업체 예시");
            detail.createCell(14).setCellValue(-90);
            detailSheet.createRow(5).createCell(1).setCellValue("내자 합계");
            detailSheet.createRow(6).createCell(1).setCellValue("외자 합계");
            detailSheet.createRow(7).createCell(1).setCellValue("합계");
            detailSheet.createRow(8).createCell(1).setCellValue("[주요 Issue 사항]");
            detailSheet.createRow(9).createCell(1).setCellValue("① 주요 이슈 예시");
            detailSheet.createRow(10).createCell(1).setCellValue("[향후 개선 방향]");
            detailSheet.createRow(11).createCell(1).setCellValue("- 개선 예시");

            workbook.write(output);
            return new MockMultipartFile("file", filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
