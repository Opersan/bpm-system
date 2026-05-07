package com.erp.modules.procurement.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDto {
    private Long itemId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal vatRate;
}
