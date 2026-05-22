package com.erp.modules.purchaserequest.dto;

import com.erp.modules.purchaserequest.entity.PurchaseRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TaskItemDto {

    private Long id;
    private String requestNumber;
    private String description;
    private String requestedBy;
    private String department;
    private LocalDate requiredBy;
    private PurchaseRequestStatus status;
    private LocalDateTime createdAt;
    private int itemCount;
    private int quoteCount;

    /** Human-readable action label shown on the task card — set by service based on role */
    private String actionLabel;

    public String getActionLabel() {
        if (actionLabel != null) return actionLabel;
        // fallback
        return switch (status) {
            case SUBMITTED -> "Teklif Gir";
            case QUOTES_ENTERED -> "İncele ve Onayla";
            case APPROVED, REJECTED -> "Görüntüle";
        };
    }

    public boolean isActionable() {
        return status == PurchaseRequestStatus.SUBMITTED || status == PurchaseRequestStatus.QUOTES_ENTERED;
    }
}
