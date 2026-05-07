package com.erp.modules.production.dto;

import com.erp.modules.production.model.ProductionLineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductionLineDto {
    private String code;
    private String name;
    private BigDecimal capacity;
    private String capacityUnit;
    private ProductionLineStatus status;
    private String responsibleTeam;
    private boolean suitable;

    public String getSuitabilityLabel() {
        return suitable ? "Planlanabilir" : "Kısıtlı";
    }
}