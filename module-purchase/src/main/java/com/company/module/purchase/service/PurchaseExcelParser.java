package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PurchaseExcelParser {

    private static final String SUMMARY_SHEET = "구매실적요약";
    private static final String DETAIL_SHEET = "기계설비공사";
    private static final String ITEM_NAME = "기계설비공사";
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(?:'|20)?(\\d{2,4})년\\s*(\\d{1,2})월");
    private static final int MAX_ROWS = 20_000;

    public ParsedPurchaseWorkbook parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet summary = workbook.getSheet(SUMMARY_SHEET);
            Sheet detail = workbook.getSheet(DETAIL_SHEET);
            if (summary == null || detail == null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            int[] period = readPeriod(summary, formatter, evaluator);
            BigDecimal plan = readSummaryAmount(summary, evaluator, 4);
            BigDecimal actual = readSummaryAmount(summary, evaluator, 7);
            List<ParsedPurchaseLine> lines = readLines(detail, formatter, evaluator);
            if (lines.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            BigDecimal detailPlan = lines.stream().map(ParsedPurchaseLine::planAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal detailActual = lines.stream().map(ParsedPurchaseLine::actualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (detailPlan.subtract(plan).abs().compareTo(new BigDecimal("0.01")) > 0
                || detailActual.subtract(actual).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return new ParsedPurchaseWorkbook(period[0], period[1], plan, actual, lines);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private int[] readPeriod(Sheet summary, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int rowIndex = 0; rowIndex < Math.min(summary.getLastRowNum() + 1, 8); rowIndex++) {
            Row row = summary.getRow(rowIndex);
            String title = text(row == null ? null : row.getCell(0), formatter, evaluator);
            Matcher matcher = PERIOD_PATTERN.matcher(title);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                if (year < 100) year += 2000;
                int month = Integer.parseInt(matcher.group(2));
                if (month < 1 || month > 12 || year < 2000 || year > 2200) break;
                return new int[]{year, month};
            }
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private BigDecimal readSummaryAmount(Sheet summary, FormulaEvaluator evaluator, int columnIndex) {
        for (int rowIndex = 0; rowIndex <= summary.getLastRowNum(); rowIndex++) {
            Row row = summary.getRow(rowIndex);
            if (row == null || !ITEM_NAME.equals(normalize(cellText(row.getCell(0), evaluator)))) continue;
            return number(row.getCell(columnIndex), evaluator);
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private List<ParsedPurchaseLine> readLines(Sheet detail, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<ParsedPurchaseLine> lines = new ArrayList<>();
        int end = Math.min(detail.getLastRowNum(), MAX_ROWS);
        for (int rowIndex = 0; rowIndex <= end; rowIndex++) {
            Row row = detail.getRow(rowIndex);
            if (row == null || !ITEM_NAME.equals(normalize(text(row.getCell(1), formatter, evaluator)))) continue;
            String supplier = text(row.getCell(13), formatter, evaluator);
            if (supplier.isBlank()) continue;
            lines.add(new ParsedPurchaseLine(
                rowIndex + 1,
                emptyToNull(text(row.getCell(2), formatter, evaluator)),
                emptyToNull(text(row.getCell(3), formatter, evaluator)),
                emptyToNull(text(row.getCell(4), formatter, evaluator)),
                emptyToNull(text(row.getCell(5), formatter, evaluator)),
                number(row.getCell(6), evaluator),
                number(row.getCell(7), evaluator),
                number(row.getCell(8), evaluator),
                number(row.getCell(9), evaluator),
                number(row.getCell(10), evaluator),
                number(row.getCell(11), evaluator),
                emptyToNull(text(row.getCell(12), formatter, evaluator)),
                supplier
            ));
        }
        return lines;
    }

    private String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private String cellText(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        return cell.toString();
    }

    private BigDecimal number(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return BigDecimal.ZERO;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            var value = evaluator.evaluate(cell);
            if (value == null || value.getCellType() == CellType.BLANK) return BigDecimal.ZERO;
            if (value.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(value.getNumberValue());
            if (value.getCellType() == CellType.STRING) return parseDecimal(value.getStringValue());
            return BigDecimal.ZERO;
        }
        if (type == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        if (type == CellType.STRING) return parseDecimal(cell.getStringCellValue());
        return BigDecimal.ZERO;
    }

    private BigDecimal parseDecimal(String value) {
        String normalized = value == null ? "" : value.replace(",", "").trim();
        return normalized.isBlank() ? BigDecimal.ZERO : new BigDecimal(normalized);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("\n", " ").trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
