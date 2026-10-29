package com.company.module.purchase.service;

import com.company.core.common.exception.BusinessException;
import com.company.core.common.exception.ErrorCode;
import com.company.module.purchase.entity.PurchaseVendorEvaluation;
import com.company.module.purchase.repository.PurchaseVendorEvaluationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseVendorEvaluationExcelService {

    private static final int MAX_EXPORT_COUNT = 100;
    private static final String ACTIVE = "N";

    private final PurchaseVendorEvaluationRepository evaluationRepository;
    private final ObjectMapper objectMapper;

    public byte[] export(Integer year, String evaluationType, List<Long> requestedIds) {
        String type = evaluationType == null ? "" : evaluationType.trim().toUpperCase(java.util.Locale.ROOT);
        if (year == null || year < 2000 || year > 2200
                || !List.of("MECHANICAL", "MAINTENANCE").contains(type)
                || requestedIds == null || requestedIds.isEmpty() || requestedIds.size() > MAX_EXPORT_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<Long> ids = requestedIds.stream().distinct().toList();
        if (ids.size() != requestedIds.size() || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        List<PurchaseVendorEvaluation> evaluations = evaluationRepository
            .findByPurchaseVendorEvaluationIdInAndEvaluationYearAndEvaluationTypeAndDeletedYn(
                ids, year, type, ACTIVE);
        if (evaluations.size() != ids.size()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle valueStyle = valueStyle(workbook);
            CellStyle numberStyle = numberStyle(workbook);
            CellStyle totalStyle = totalStyle(workbook, numberStyle);
            Map<String, Integer> sheetNameCounts = new HashMap<>();

            for (PurchaseVendorEvaluation evaluation : evaluations) {
                writeEvaluationSheet(workbook, evaluation, titleStyle, headerStyle, valueStyle, numberStyle,
                    totalStyle, sheetNameCounts);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void writeEvaluationSheet(XSSFWorkbook workbook, PurchaseVendorEvaluation evaluation,
                                      CellStyle titleStyle, CellStyle headerStyle, CellStyle valueStyle,
                                      CellStyle numberStyle, CellStyle totalStyle,
                                      Map<String, Integer> sheetNameCounts) throws IOException {
        Sheet sheet = workbook.createSheet(sheetName(evaluation, sheetNameCounts));
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(typeLabel(evaluation.getEvaluationType()) + " 협력업체 평가표");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        Row details = sheet.createRow(1);
        setText(details, 0, "평가 연도", valueStyle);
        setText(details, 1, String.valueOf(evaluation.getEvaluationYear()), valueStyle);
        setText(details, 2, "구분", valueStyle);
        setText(details, 3, typeLabel(evaluation.getEvaluationType()), valueStyle);
        setText(details, 4, "평가일", valueStyle);
        setText(details, 5, evaluation.getEvaluationDate().toString(), valueStyle);

        Row supplier = sheet.createRow(2);
        setText(supplier, 0, "협력업체", valueStyle);
        setText(supplier, 1, evaluation.getSupplierName(), valueStyle);
        setText(supplier, 2, "평가자", valueStyle);
        setText(supplier, 3, evaluation.getEvaluatorName(), valueStyle);
        setText(supplier, 4, "등급", valueStyle);
        setText(supplier, 5, evaluation.getFinalGrade(), valueStyle);

        String[] headers = {"번호", "평가기준", "평가항목", "배점", "평가결과", "평가점수"};
        Row header = sheet.createRow(4);
        for (int index = 0; index < headers.length; index++) {
            setText(header, index, headers[index], headerStyle);
        }

        Map<String, String> answers = objectMapper.readValue(evaluation.getAnswersJson(),
            new TypeReference<Map<String, String>>() { });
        List<PurchaseVendorEvaluationScoring.ScoredCriterion> scoredCriteria = objectMapper.readValue(
            evaluation.getScoresJson(), new TypeReference<List<PurchaseVendorEvaluationScoring.ScoredCriterion>>() { });
        Map<String, PurchaseVendorEvaluationScoring.ScoredCriterion> scoresByCode = new HashMap<>();
        scoredCriteria.forEach(score -> scoresByCode.put(score.code(), score));

        int rowIndex = 5;
        int number = 1;
        for (PurchaseVendorEvaluationScoring.Criterion criterion : PurchaseVendorEvaluationScoring.criteria()) {
            PurchaseVendorEvaluationScoring.ScoredCriterion score = scoresByCode.get(criterion.code());
            if (score == null) throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            Row row = sheet.createRow(rowIndex++);
            setText(row, 0, String.valueOf(number++), valueStyle);
            setText(row, 1, criterion.section(), valueStyle);
            setText(row, 2, criterion.name(), valueStyle);
            setNumber(row, 3, criterion.maximumScore(), numberStyle);
            setText(row, 4, score.optionLabel(), valueStyle);
            setNumber(row, 5, score.score(), numberStyle);
            if (!score.optionCode().equals(answers.get(criterion.code()))) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }

        Row total = sheet.createRow(rowIndex + 1);
        setText(total, 2, "총점 / 등급", totalStyle);
        setNumber(total, 5, evaluation.getTotalScore(), totalStyle);
        Row grade = sheet.createRow(rowIndex + 2);
        setText(grade, 2, "최종 등급", totalStyle);
        setText(grade, 5, evaluation.getFinalGrade(), totalStyle);

        int[] widths = {8, 20, 36, 12, 34, 14};
        for (int index = 0; index < widths.length; index++) sheet.setColumnWidth(index, widths[index] * 256);
        sheet.createFreezePane(0, 5);
    }

    private String sheetName(PurchaseVendorEvaluation evaluation, Map<String, Integer> counts) {
        String base = typeLabel(evaluation.getEvaluationType()) + "_" + evaluation.getSupplierName();
        String safe = WorkbookUtil.createSafeSheetName(base, '_').trim();
        if (safe.isEmpty()) safe = "평가서";
        String suffix = "_" + evaluation.getPurchaseVendorEvaluationId();
        if (safe.length() + suffix.length() > 31) safe = safe.substring(0, 31 - suffix.length());
        String candidate = safe + suffix;
        int count = counts.merge(candidate, 1, Integer::sum);
        return count == 1 ? candidate : safe.substring(0, Math.min(safe.length(), 26)) + "_" + count + suffix;
    }

    private String typeLabel(String type) {
        return "MAINTENANCE".equals(type) ? "유지보수" : "기계설비";
    }

    private void setText(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void setNumber(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? 0 : value.doubleValue());
        cell.setCellStyle(style);
    }

    private CellStyle titleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
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
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle valueStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private CellStyle numberStyle(XSSFWorkbook workbook) {
        CellStyle style = valueStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.0#"));
        return style;
    }

    private CellStyle totalStyle(XSSFWorkbook workbook, CellStyle numberStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(numberStyle);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
