package com.erp.modules.planning.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class WarehouseSummaryDto {
    private final String warehouse;
    private final long itemCount;
    private final long criticalItemCount;
    private final BigDecimal totalStockValue;
}
