package com.company.module.purchase.controller;

import com.company.core.common.response.ApiResponse;
import com.company.module.purchase.dto.PurchaseCategoryResponse;
import com.company.module.purchase.dto.PurchaseDashboardResponse;
import com.company.module.purchase.dto.PurchaseUploadResponse;
import com.company.module.purchase.service.PurchaseDashboardService;
import com.company.module.purchase.service.PurchaseUploadService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/purchase-api")
public class PurchaseController {

    private final PurchaseUploadService uploadService;
    private final PurchaseDashboardService dashboardService;

    @PostMapping("/uploads")
    @PreAuthorize("hasAuthority('PURCHASE_WRITE')")
    public ResponseEntity<ApiResponse<PurchaseUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean confirmReplace) {
        return ResponseEntity.ok(ApiResponse.success(uploadService.upload(file, confirmReplace)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<PurchaseDashboardResponse>> dashboard(
            @RequestParam @Min(2000) @Max(2200) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(year)));
    }

    @GetMapping("/details")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<ApiResponse<Page<PurchaseCategoryResponse>>> detail(
            @RequestParam @Min(2000) @Max(2200) Integer year,
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) String categoryName,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(
            uploadService.detail(year, month, categoryName, page, size)));
    }
}
