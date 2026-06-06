package com.erp.modules.production.entity;

import com.erp.modules.common.BaseEntity;
import com.erp.modules.procurement.entity.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "work_order_material_requirements")
public class WorkOrderMaterialRequirement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "work_order_operation_id", nullable = false)
    private com.erp.modules.production.entity.WorkOrderOperation workOrderOperation;

    @ManyToOne
    @JoinColumn(name = "operation_material_requirement_id")
    private OperationMaterialRequirement operationMaterialRequirement;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "standard_quantity_per_operation", nullable = false)
    private BigDecimal standardQuantityPerOperation;

    @Column(name = "total_required_quantity", nullable = false)
    private BigDecimal totalRequiredQuantity;

    @Column(name = "scrap_rate")
    private BigDecimal scrapRate = BigDecimal.ZERO;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WorkOrderMaterialRequirementStatus status = WorkOrderMaterialRequirementStatus.PLANNED;

    private String description;
}
