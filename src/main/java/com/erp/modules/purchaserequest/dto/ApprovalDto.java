package com.erp.modules.purchaserequest.dto;

import lombok.Data;

@Data
public class ApprovalDto {
    private Long selectedQuoteId;
    private String notes;
    private String rejectionReason;
}
