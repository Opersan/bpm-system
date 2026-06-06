package com.erp.modules.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OperationDto {

    private Long id;

    private String code;

    private String name;

    private String description;

    private BigDecimal standardDuration = BigDecimal.ZERO;

    private String durationUnit = "MINUTES";

    private Integer defaultSequence = 1;

    private String workCenter;

    private BigDecimal capacity = BigDecimal.ZERO;

    private String capacityUnit;

    private boolean active = true;

    @Builder.Default
    private List<OperationMaterialRequirementDto> materialRequirements = new ArrayList<>();

    private int materialRequirementCount;

    public String getDurationLabel() {
        return standardDuration + " " + durationUnit;
    }
}
