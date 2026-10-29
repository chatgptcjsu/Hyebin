package com.company.module.purchase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

public record PurchaseVendorEvaluationRequest(
    @NotNull @Min(2000) @Max(2200) Integer evaluationYear,
    @NotBlank @Size(max = 100) String evaluationType,
    @NotBlank @Size(max = 200) String supplierName,
    @NotBlank @Size(max = 100) String evaluatorName,
    @NotNull LocalDate evaluationDate,
    @Size(max = 2000) String evaluationOpinion,
    @NotEmpty @Size(min = 12, max = 12) Map<String, String> answers
) { }
