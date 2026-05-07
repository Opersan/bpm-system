package com.erp.modules.procurement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePORequest {
    private Long supplierId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String currency;
    private String description;
    private String createdBy;
    private List<PurchaseOrderItemDto> items;
}
