package com.erp.modules.production.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductionSummaryDto {
    private final String label;
    private final String value;
    private final String icon;
    private final String variant;
}