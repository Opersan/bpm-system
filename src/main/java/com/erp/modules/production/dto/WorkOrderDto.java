package com.erp.modules.production.dto;

import com.erp.modules.production.model.WorkOrderPriority;
import com.erp.modules.production.model.WorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDto {
    private String workOrderNo;
    private ProductDto product;
    private BigDecimal plannedQuantity;
    private BigDecimal producedQuantity;
    private String unit;
    private ProductionLineDto productionLine;
    private WorkOrderPriority priority;
    private WorkOrderStatus status;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private String responsible;
    private String shift;
    private String description;
    @Builder.Default
    private List<WorkOrderMaterialRequirementDto> materialRequirements = new ArrayList<>();
    private int completionRate;
    private boolean delayed;

    public boolean isStartAllowed() {
        return status == WorkOrderStatus.DRAFT
            || status == WorkOrderStatus.PLANNED
            || status == WorkOrderStatus.PAUSED
            || status == WorkOrderStatus.DELAYED;
    }

    public boolean isPauseAllowed() {
        return status == WorkOrderStatus.IN_PROGRESS;
    }

    public boolean isCompleteAllowed() {
        return status == WorkOrderStatus.IN_PROGRESS;
    }

    public boolean isCancelAllowed() {
        return status != null && !status.isTerminal();
    }

    public boolean isMaterialRisk() {
        return materialRequirements != null && materialRequirements.stream().anyMatch(WorkOrderMaterialRequirementDto::isAttentionRequired);
    }
}