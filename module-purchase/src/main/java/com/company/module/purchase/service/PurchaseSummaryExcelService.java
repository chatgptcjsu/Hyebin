package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseSummaryExcelService {

    private static final String[] HEADERS = {"월", "계획", "실적", "계획대비차이", "전월대비차이"};
    private static final String AMOUNT_FORMAT = "#,##0;[Red](#,##0);0";

    private final PurchaseDashboardService dashboardService;

    public byte[] export(Integer year) {
        PurchaseDashboardResponse dashboard = dashboardService.getDashboard(year);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("월별 구매실적 요약");
            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle amountStyle = amountStyle(workbook);
            CellStyle totalStyle = totalStyle(workbook, amountStyle);

            Row title = sheet.createRow(0);
            Cell titleCell = title.createCell(0);
            titleCell.setCellValue(year + "년 월별 구매실적 요약");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            Row unit = sheet.createRow(1);
            unit.createCell(0).setCellValue("금액 단위: 백만원 · VAT 별도");

            Row header = sheet.createRow(3);
            for (int column = 0; column < HEADERS.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(HEADERS[column]);
                cell.setCellStyle(headerStyle);
            }

            List<PurchaseDashboardResponse.MonthPoint> points = dashboard.getMonthly();
            int rowIndex = 4;
            for (PurchaseDashboardResponse.MonthPoint point : points) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(point.getMonth() + "월");
                if (point.isUploaded()) {
                    setAmount(row.createCell(1), point.getPlanAmount(), amountStyle);
                    setAmount(row.createCell(2), point.getActualAmount(), amountStyle);
                    setAmount(row.createCell(3), point.getVarianceAmount(), amountStyle);
                    setAmount(row.createCell(4), point.getPreviousMonthVarianceAmount(), amountStyle);
                }
            }

            Row total = sheet.createRow(rowIndex);
            total.createCell(0).setCellValue("누계");
            setAmount(total.createCell(1), dashboard.getTotalPlanAmount(), totalStyle);
            setAmount(total.createCell(2), dashboard.getTotalActualAmount(), totalStyle);
            setAmount(total.createCell(3), dashboard.getTotalVarianceAmount(), totalStyle);
            setAmount(total.createCell(4), dashboard.getTotalPreviousMonthVarianceAmount(), totalStyle);

            sheet.setColumnWidth(0, 14 * 256);
            for (int column = 1; column < HEADERS.length; column++) {
                sheet.setColumnWidth(column, 20 * 256);
            }
            sheet.createFreezePane(0, 4);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void setAmount(Cell cell, BigDecimal amount, CellStyle style) {
        cell.setCellValue(amount == null ? 0 : amount.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 15);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle amountStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(AMOUNT_FORMAT));
        return style;
    }

    private CellStyle totalStyle(XSSFWorkbook workbook, CellStyle amountStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(amountStyle);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
