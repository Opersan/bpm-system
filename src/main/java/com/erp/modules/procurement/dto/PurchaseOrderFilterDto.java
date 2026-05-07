package com.erp.modules.procurement.dto;

import com.erp.modules.procurement.entity.ApprovalStatus;
import com.erp.modules.procurement.entity.POStatus;
import lombok.Data;

@Data
public class PurchaseOrderFilterDto {
    private String search;
    private POStatus status;
    private ApprovalStatus approvalStatus;
    private Long supplierId;
}
