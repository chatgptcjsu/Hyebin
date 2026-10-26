package com.company.module.purchase.controller;

import com.company.core.common.response.ApiResponse;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import com.company.module.purchase.dto.PurchaseDetailSheetResponse;
import com.company.module.purchase.dto.PurchaseUploadHistoryResponse;
import com.company.module.purchase.dto.PurchaseUploadResponse;
import com.company.module.purchase.service.PurchaseDashboardService;
import com.company.module.purchase.service.PurchaseDetailSheetService;
import com.company.module.purchase.service.PurchaseSummaryExcelService;
import com.company.module.purchase.service.PurchaseUploadHistoryService;
import com.company.module.purchase.service.PurchaseUploadService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;


@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/purchase-api")
public class PurchaseController {

    private final PurchaseUploadService uploadService;
    private final PurchaseDashboardService dashboardService;
    private final PurchaseDetailSheetService detailSheetService;
    private final PurchaseSummaryExcelService summaryExcelService;
    private final PurchaseUploadHistoryService uploadHistoryService;

    @PostMapping("/uploads")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PurchaseUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "confirmReplace", defaultValue = "false") boolean confirmReplace) {
        return ResponseEntity.ok(ApiResponse.success(uploadService.upload(file, confirmReplace)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<PurchaseDashboardResponse>> dashboard(
            @RequestParam("year") @Min(2000) @Max(2200) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(year)));
    }

    @GetMapping("/detail-sheet")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<PurchaseDetailSheetResponse>> detailSheet(
            @RequestParam("year") @Min(2000) @Max(2200) Integer year,
            @RequestParam("month") @Min(1) @Max(12) Integer month) {
        return ResponseEntity.ok(ApiResponse.success(detailSheetService.getCurrentSheet(year, month)));
    }

    @GetMapping("/upload-history")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<List<PurchaseUploadHistoryResponse>>> uploadHistory(
            @RequestParam("year") @Min(2000) @Max(2200) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(uploadHistoryService.getHistory(year)));
    }

    @GetMapping("/dashboard/export")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<byte[]> exportDashboard(
            @RequestParam("year") @Min(2000) @Max(2200) Integer year) {
        byte[] workbook = summaryExcelService.export(year);
        String filename = year + "년_월별_구매실적_요약.xlsx";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(workbook);
    }

}
