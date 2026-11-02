package com.company.module.purchase.dto;

public record PurchaseVendorEvaluationFormTypeOption(Long purchaseVendorEvaluationFormTypeId, String formTypeName,
                                                      String originalFilename, String fileContentType,
                                                      String fileDownloadUrl, boolean hasFile) { }
