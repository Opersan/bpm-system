package com.erp.modules.planning.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MrpResultDto {
    private final String materialCode;
    private final String materialName;
    private final String category;
    private final BigDecimal demand;
    private final BigDecimal currentStock;
    private final BigDecimal safetyStock;
    private final BigDecimal openPurchaseOrders;
    private final BigDecimal openWorkOrders;
    private final BigDecimal netRequirement;
    private final BigDecimal suggestedQuantity;
    private final LocalDate suggestedDate;
    private final MrpActionType actionType;
    private final MrpStatus status;
    private final BigDecimal estimatedCost;
}
