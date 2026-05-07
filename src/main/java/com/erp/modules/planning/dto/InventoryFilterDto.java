package com.erp.modules.planning.dto;

import lombok.Data;

@Data
public class InventoryFilterDto {
    private String search;
    private String category;
    private String warehouse;
    private InventoryStatus status;
    private boolean criticalOnly;
}
