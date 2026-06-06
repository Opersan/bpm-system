package com.erp.modules.inventory.dto;

import com.erp.modules.inventory.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDto {
    private Long id;
    private LocalDateTime createdAt;
    private TransactionType type;
    private Integer quantity;
    private Integer previousQuantity;
    private Integer newQuantity;
    private String warehouse;
    private Long referenceId;
    private String referenceType;
    private String note;
    private String createdBy;
}
