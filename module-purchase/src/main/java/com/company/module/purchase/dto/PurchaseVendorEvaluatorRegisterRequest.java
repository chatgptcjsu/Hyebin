package com.company.module.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PurchaseVendorEvaluatorRegisterRequest(
    @NotBlank @Size(max = 100) String evaluatorName
) { }
