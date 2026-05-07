package com.erp.modules.procurement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProcurementSummaryCardDto {
    private final String label;
    private final String value;
    private final String icon;
    private final String variant;
}
