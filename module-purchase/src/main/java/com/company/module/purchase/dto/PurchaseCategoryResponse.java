package com.company.module.purchase.dto;

import com.company.module.purchase.entity.PurchaseCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PurchaseCategoryResponse {
    private Integer sourceRowNo;
    private String categoryName;
    private BigDecimal previousActualAmount;
    private BigDecimal planForeignAmount;
    private BigDecimal planDomesticAmount;
    private BigDecimal planAmount;
    private BigDecimal actualForeignAmount;
    private BigDecimal actualDomesticAmount;
    private BigDecimal actualAmount;
    private BigDecimal planVarianceAmount;
    private BigDecimal previousMonthVarianceAmount;
    private String planVarianceReason;
    private String previousMonthVarianceReason;

    public static PurchaseCategoryResponse from(PurchaseCategory category) {
        return PurchaseCategoryResponse.builder()
            .sourceRowNo(category.getSourceRowNo())
            .categoryName(category.getCategoryName())
            .previousActualAmount(category.getPreviousActualAmount())
            .planForeignAmount(category.getPlanForeignAmount())
            .planDomesticAmount(category.getPlanDomesticAmount())
            .planAmount(category.getPlanAmount())
            .actualForeignAmount(category.getActualForeignAmount())
            .actualDomesticAmount(category.getActualDomesticAmount())
            .actualAmount(category.getActualAmount())
            .planVarianceAmount(category.getPlanVarianceAmount())
            .previousMonthVarianceAmount(category.getPreviousMonthVarianceAmount())
            .planVarianceReason(category.getPlanVarianceReason())
            .previousMonthVarianceReason(category.getPreviousMonthVarianceReason())
            .build();
    }
}
