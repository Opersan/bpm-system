package com.erp.modules.planning.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MrpRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private String productGroup;
    private String warehouse;
    private boolean includeSafetyStock = true;
    private boolean includeOpenPurchaseOrders = true;
    private boolean includeOpenWorkOrders = true;
}
