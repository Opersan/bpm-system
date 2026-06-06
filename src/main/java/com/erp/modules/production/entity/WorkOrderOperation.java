package com.erp.modules.production.entity;

import com.erp.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_order_operations")
public class WorkOrderOperation extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "work_order_id", nullable = false)
    private com.erp.modules.manufacturing.entity.WorkOrder workOrder;

    @ManyToOne
    @JoinColumn(name = "operation_id", nullable = false)
    private Operation operation;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "planned_duration", nullable = false)
    private BigDecimal plannedDuration = BigDecimal.ZERO;

    @Column(name = "duration_unit", nullable = false)
    private String durationUnit = "MINUTES";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkOrderOperationStatus status = WorkOrderOperationStatus.PENDING;

    private String description;

    @OneToMany(mappedBy = "workOrderOperation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderMaterialRequirement> materialRequirements = new ArrayList<>();
}
