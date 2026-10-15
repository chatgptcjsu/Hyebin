package com.company.module.purchase.dto;

import com.company.module.purchase.entity.PurchaseLine;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PurchaseLineResponse {
    private Integer sourceRowNo;
    private String businessUnit;
    private String importType;
    private String supplierCode;
    private String unitName;
    private BigDecimal previousQuantity;
    private BigDecimal previousAmount;
    private BigDecimal planQuantity;
    private BigDecimal planAmount;
    private BigDecimal actualQuantity;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount;
    private String noteText;
    private String supplierName;

    public static PurchaseLineResponse from(PurchaseLine line) {
        BigDecimal plan = line.getPlanAmount() == null ? BigDecimal.ZERO : line.getPlanAmount();
        BigDecimal actual = line.getActualAmount() == null ? BigDecimal.ZERO : line.getActualAmount();
        return PurchaseLineResponse.builder()
            .sourceRowNo(line.getSourceRowNo())
            .businessUnit(line.getBusinessUnit())
            .importType(line.getImportType())
            .supplierCode(line.getSupplierCode())
            .unitName(line.getUnitName())
            .previousQuantity(line.getPreviousQuantity())
            .previousAmount(line.getPreviousAmount())
            .planQuantity(line.getPlanQuantity())
            .planAmount(line.getPlanAmount())
            .actualQuantity(line.getActualQuantity())
            .actualAmount(line.getActualAmount())
            .varianceAmount(actual.subtract(plan))
            .noteText(line.getNoteText())
            .supplierName(line.getSupplierName())
            .build();
    }
}
