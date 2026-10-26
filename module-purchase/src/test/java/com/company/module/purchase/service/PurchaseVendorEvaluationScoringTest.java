package com.company.module.purchase.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseVendorEvaluationScoringTest {

    @Test
    void scoresAllMaximumSelectionsAsOneHundredAndGradeA() {
        Map<String, String> answers = new HashMap<>();
        for (var criterion : PurchaseVendorEvaluationScoring.criteria()) {
            answers.put(criterion.code(), criterion.options().get(0).code());
        }

        var result = PurchaseVendorEvaluationScoring.calculate(answers);

        assertEquals(new BigDecimal("100"), result.totalScore());
        assertEquals("A", result.grade());
        assertEquals(12, result.criteria().size());
    }

    @Test
    void followsExcelPointValuesAndGradeThresholds() {
        Map<String, String> answers = Map.ofEntries(
            Map.entry("creditRating", "CCC_PLUS_OR_LOWER_OR_MISSING"),
            Map.entry("qualityCertification", "NONE_OR_MISSING"),
            Map.entry("qualityContribution", "QUALITY_MAINTAINED"),
            Map.entry("industryExperience", "UNDER_THREE_YEARS"),
            Map.entry("otherCustomerCount", "UNDER_TWO_OR_NO_REPLY"),
            Map.entry("productQuality", "GOOD"),
            Map.entry("performance", "GOOD"),
            Map.entry("defectCount", "ZERO"),
            Map.entry("ownProductionRate", "NOT_APPLICABLE"),
            Map.entry("deliveryCompliance", "ONE_TO_TWO_MISSES"),
            Map.entry("afterService", "GOOD"),
            Map.entry("workCooperation", "GOOD")
        );

        var result = PurchaseVendorEvaluationScoring.calculate(answers);

        assertEquals(new BigDecimal("82.5"), result.totalScore());
        assertEquals("B", result.grade());
        assertEquals("D", PurchaseVendorEvaluationScoring.grade(new BigDecimal("69.5")));
        assertEquals("C", PurchaseVendorEvaluationScoring.grade(new BigDecimal("70")));
        assertEquals("B", PurchaseVendorEvaluationScoring.grade(new BigDecimal("80")));
        assertEquals("A", PurchaseVendorEvaluationScoring.grade(new BigDecimal("90")));
    }

    @Test
    void requiresAllTwelveValidSelections() {
        assertThrows(IllegalArgumentException.class,
            () -> PurchaseVendorEvaluationScoring.calculate(Map.of()));
        Map<String, String> invalid = new HashMap<>();
        for (var criterion : PurchaseVendorEvaluationScoring.criteria()) {
            invalid.put(criterion.code(), criterion.options().get(0).code());
        }
        invalid.put("performance", "UNSUPPORTED");
        assertThrows(IllegalArgumentException.class,
            () -> PurchaseVendorEvaluationScoring.calculate(invalid));
    }
}
