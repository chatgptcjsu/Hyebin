package com.company.module.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseVendorEvaluationExportRequest(
    @NotNull @Min(2000) @Max(2200) Integer evaluationYear,
    @NotBlank @Size(max = 100) String evaluationType,
    @NotEmpty @Size(max = 100) List<@NotNull @Min(1) Long> evaluationIds
) { }
