package com.erp.modules.planning.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SummaryCardDto {
    private final String label;
    private final String value;
    private final String icon;
    private final String variant;
}
