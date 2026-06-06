package com.erp.modules.planning.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class InventoryItemDto {
    private final Long itemId;
    private final Long warehouseId;
    private final String materialCode;
    private final String materialName;
    private final String category;
    private final BigDecimal currentStock;
    private final BigDecimal minimumStock;
    private final BigDecimal maximumStock;
    private final String unit;
    private final String warehouse;
    private final InventoryStatus status;
    private final LocalDate lastMovementDate;
    private final BigDecimal unitCost;

    public BigDecimal getStockValue() {
        return currentStock.multiply(unitCost);
    }
}
