package com.company.module.purchase.controller;

import com.company.core.common.response.ApiResponse;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import com.company.module.purchase.dto.PurchaseDetailSheetResponse;
import com.company.module.purchase.dto.PurchaseUploadHistoryResponse;
import com.company.module.purchase.dto.PurchaseUploadResponse;
import com.company.module.purchase.dto.PurchaseVendorEvaluationRequest;
import com.company.module.purchase.dto.PurchaseVendorEvaluationResponse;
import com.company.module.purchase.dto.PurchaseVendorEvaluationExportRequest;
import com.company.module.purchase.dto.PurchaseVendorEvaluatorOption;
import com.company.module.purchase.dto.PurchaseVendorEvaluatorRegisterRequest;
import com.company.module.purchase.service.PurchaseDashboardService;
import com.company.module.purchase.service.PurchaseDetailSheetService;
import com.company.module.purchase.service.PurchaseSummaryExcelService;
import com.company.module.purchase.service.PurchaseUploadHistoryService;
import com.company.module.purchase.service.PurchaseUploadService;
import com.company.module.purchase.service.PurchaseVendorEvaluationService;
import com.company.module.purchase.service.PurchaseVendorEvaluationExcelService;
import com.company.module.purchase.service.PurchaseVendorEvaluatorRegistryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final PurchaseVendorEvaluationService vendorEvaluationService;
    private final PurchaseVendorEvaluationExcelService vendorEvaluationExcelService;
    private final PurchaseVendorEvaluatorRegistryService vendorEvaluatorRegistryService;

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

    @GetMapping("/upload-history/{uploadBatchId}/file")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<byte[]> downloadOriginalFile(@PathVariable("uploadBatchId") @Min(1) Long uploadBatchId) {
        PurchaseUploadHistoryService.DownloadedFile file = uploadHistoryService.getOriginalFile(uploadBatchId)
            .orElse(null);
        if (file == null) return ResponseEntity.notFound().build();

        String contentType = file.filename().toLowerCase().endsWith(".xls")
            ? "application/vnd.ms-excel"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(file.bytes());
    }

    @GetMapping("/vendor-evaluators")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<List<PurchaseVendorEvaluatorOption>>> vendorEvaluators() {
        return ResponseEntity.ok(ApiResponse.success(vendorEvaluatorRegistryService.getEvaluators()));
    }

    @PostMapping("/vendor-evaluators")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PurchaseVendorEvaluatorOption>> registerVendorEvaluator(
            @Valid @RequestBody PurchaseVendorEvaluatorRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(vendorEvaluatorRegistryService.register(request)));
    }

    @GetMapping("/vendor-evaluations/criteria")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<List<com.company.module.purchase.service.PurchaseVendorEvaluationScoring.Criterion>>> vendorEvaluationCriteria() {
        return ResponseEntity.ok(ApiResponse.success(vendorEvaluationService.getCriteria()));
    }

    @GetMapping("/vendor-evaluations")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<List<PurchaseVendorEvaluationResponse>>> vendorEvaluations(
            @RequestParam("year") @Min(2000) @Max(2200) Integer year,
            @RequestParam("evaluationType") String evaluationType) {
        return ResponseEntity.ok(ApiResponse.success(vendorEvaluationService.getEvaluations(year, evaluationType)));
    }

    @GetMapping("/vendor-evaluations/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<PurchaseVendorEvaluationResponse>> vendorEvaluation(@PathVariable("id") @Min(1) Long id) {
        return ResponseEntity.ok(ApiResponse.success(vendorEvaluationService.getEvaluation(id)));
    }

    @PostMapping("/vendor-evaluations/export")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<byte[]> exportVendorEvaluations(@Valid @RequestBody PurchaseVendorEvaluationExportRequest request) {
        byte[] workbook = vendorEvaluationExcelService.export(
            request.evaluationYear(), request.evaluationType(), request.evaluationIds());
        String typeLabel = "MAINTENANCE".equalsIgnoreCase(request.evaluationType()) ? "유지보수" : "기계설비공사";
        String filename = request.evaluationYear() + "년_" + typeLabel + "_협력업체_평가표.xlsx";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(workbook);
    }

    @PostMapping("/vendor-evaluations")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PurchaseVendorEvaluationResponse>> createVendorEvaluation(
            @Valid @RequestBody PurchaseVendorEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(vendorEvaluationService.create(request)));
    }

    @PutMapping("/vendor-evaluations/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PurchaseVendorEvaluationResponse>> updateVendorEvaluation(
            @PathVariable("id") @Min(1) Long id,
            @Valid @RequestBody PurchaseVendorEvaluationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(vendorEvaluationService.update(id, request)));
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
