package com.erp.modules.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockOperationRequest {
    private Long itemId;
    private Long warehouseId;
    private Integer quantity;
    private String note;
}
