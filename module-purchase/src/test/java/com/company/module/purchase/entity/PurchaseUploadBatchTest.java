package com.company.module.purchase.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseUploadBatchTest {

    @Test
    void newUploadVersionStartsWithItsLatestUploadTimestamp() {
        PurchaseUploadBatch batch = PurchaseUploadBatch.builder()
            .purchaseYear(2026)
            .purchaseMonth(7)
            .originalFilename("2026-07.xlsx")
            .fileSha256("a".repeat(64))
            .planAmount(BigDecimal.ZERO)
            .actualAmount(BigDecimal.ZERO)
            .summaryRowCount(1)
            .build();
        ReflectionTestUtils.invokeMethod(batch, "onCreate");
        assertTrue(batch.getLastUploadedAt().equals(batch.getCreatedAt()));
    }
}
