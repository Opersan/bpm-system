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
@Table(name = "operation_material_requirements")
public class OperationMaterialRequirement extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "operation_id", nullable = false)
    private Operation operation;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "required_quantity", nullable = false)
    private BigDecimal requiredQuantity;

    private String unit;

    @Column(name = "scrap_rate")
    private BigDecimal scrapRate = BigDecimal.ZERO;

    @Column(name = "is_critical")
    private boolean critical = false;

    private String description;
}
