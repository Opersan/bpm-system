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
@Table(name = "operations")
public class Operation extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "standard_duration", nullable = false)
    private BigDecimal standardDuration = BigDecimal.ZERO;

    @Column(name = "duration_unit", nullable = false)
    private String durationUnit = "MINUTES";

    @Column(name = "default_sequence", nullable = false)
    private Integer defaultSequence = 1;

    private String workCenter;

    private BigDecimal capacity;

    private String capacityUnit;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "operation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationMaterialRequirement> materialRequirements = new ArrayList<>();
}
