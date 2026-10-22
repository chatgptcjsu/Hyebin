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

    private static final String TOTAL_LABEL = "합계";
    private static final Pattern PERIOD_PATTERN = Pattern.compile("['’]?(\\d{2}|\\d{4})년\\s*(\\d{1,2})월");
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");
    private static final int MAX_ROWS = 20_000;

    public ParsedPurchaseWorkbook parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() < 1) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            Sheet firstSheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();
            int[] period = readPeriod(file.getOriginalFilename(), firstSheet, formatter, evaluator);
            List<ParsedPurchaseCategory> categories = readCategories(firstSheet, formatter, evaluator);
            if (categories.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }

            BigDecimal categoryPlan = categories.stream().map(ParsedPurchaseCategory::planAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal categoryActual = categories.stream().map(ParsedPurchaseCategory::actualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPlan = readTotalAmount(firstSheet, formatter, evaluator, 4).orElse(categoryPlan);
            BigDecimal totalActual = readTotalAmount(firstSheet, formatter, evaluator, 7).orElse(categoryActual);
            if (categoryPlan.subtract(totalPlan).abs().compareTo(AMOUNT_TOLERANCE) > 0
                || categoryActual.subtract(totalActual).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            return new ParsedPurchaseWorkbook(period[0], period[1], totalPlan, totalActual, categories);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private int[] readPeriod(String filename, Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        int[] filenamePeriod = findPeriod(filename);
        if (filenamePeriod != null) return filenamePeriod;

        for (int rowIndex = 0; rowIndex < Math.min(sheet.getLastRowNum() + 1, 8); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            String title = text(row == null ? null : row.getCell(0), formatter, evaluator);
            int[] titlePeriod = findPeriod(title);
            if (titlePeriod != null) return titlePeriod;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private int[] findPeriod(String value) {
        if (value == null || value.isBlank()) return null;
        Matcher matcher = PERIOD_PATTERN.matcher(value);
        if (!matcher.find()) return null;

        int year = Integer.parseInt(matcher.group(1));
        if (year < 100) year += 2000;
        int month = Integer.parseInt(matcher.group(2));
        if (month < 1 || month > 12 || year < 2000 || year > 2200) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new int[]{year, month};
    }

    private List<ParsedPurchaseCategory> readCategories(Sheet sheet, DataFormatter formatter,
                                                         FormulaEvaluator evaluator) {
        List<ParsedPurchaseCategory> categories = new ArrayList<>();
        int end = Math.min(sheet.getLastRowNum(), MAX_ROWS);
        for (int rowIndex = 0; rowIndex <= end; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            String name = normalize(text(row.getCell(0), formatter, evaluator));
            if (name.isBlank() || "구분".equals(name) || TOTAL_LABEL.equals(name)
                || name.contains("구매실적")) continue;
            if (!hasValue(row.getCell(4)) && !hasValue(row.getCell(7))) continue;

            categories.add(new ParsedPurchaseCategory(
                rowIndex + 1,
                name,
                number(row.getCell(1), evaluator),
                number(row.getCell(2), evaluator),
                number(row.getCell(3), evaluator),
                number(row.getCell(4), evaluator),
                number(row.getCell(5), evaluator),
                number(row.getCell(6), evaluator),
                number(row.getCell(7), evaluator),
                number(row.getCell(8), evaluator),
                number(row.getCell(9), evaluator),
                emptyToNull(text(row.getCell(10), formatter, evaluator)),
                emptyToNull(text(row.getCell(11), formatter, evaluator))
            ));
        }
        return categories;
    }

    private java.util.Optional<BigDecimal> readTotalAmount(Sheet sheet, DataFormatter formatter,
                                                            FormulaEvaluator evaluator, int amountColumn) {
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), MAX_ROWS); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null && TOTAL_LABEL.equals(normalize(text(row.getCell(0), formatter, evaluator)))
                && hasValue(row.getCell(amountColumn))) {
                return java.util.Optional.of(number(row.getCell(amountColumn), evaluator));
            }
        }
        return java.util.Optional.empty();
    }

    private boolean hasValue(Cell cell) {
        return cell != null && cell.getCellType() != CellType.BLANK;
    }

    private String text(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        return formatter.formatCellValue(cell, evaluator).trim();
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
