package com.erp.modules.purchaserequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PurchaseRequestCreateDto {

    private String description;
    private String department;
    private LocalDate requiredBy;
    private List<PurchaseRequestItemDto> items = new ArrayList<>();

    @Data
    public static class PurchaseRequestItemDto {
        private Long itemId;
        private String itemName;
        private String itemCode;
        private BigDecimal quantity;
        private String uom;
        private String notes;
    }
}
