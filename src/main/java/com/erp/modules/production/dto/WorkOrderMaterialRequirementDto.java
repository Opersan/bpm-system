package com.erp.modules.production.dto;

import com.erp.modules.production.model.MaterialRequirementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderMaterialRequirementDto {
    private String materialCode;
    private String materialName;
    private BigDecimal baseQuantity;
    private BigDecimal requiredQuantity;
    private BigDecimal availableStock;
    private BigDecimal shortageQuantity;
    private String unit;
    private MaterialRequirementStatus status;

    public boolean isShortage() {
        return shortageQuantity != null && shortageQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isAttentionRequired() {
        return status == MaterialRequirementStatus.LOW_STOCK
            || status == MaterialRequirementStatus.MISSING
            || status == MaterialRequirementStatus.CRITICAL;
    }
}