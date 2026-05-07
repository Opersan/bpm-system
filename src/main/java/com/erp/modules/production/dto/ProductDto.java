package com.erp.modules.production.dto;

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
public class ProductDto {
    private String code;
    private String name;
    private String productGroup;
    private BigDecimal standardProductionHours;
    private String defaultUnit;
    private boolean bomAvailable;

    public String getBomStatusLabel() {
        return bomAvailable ? "Reçete Hazır" : "Reçete Eksik";
    }
}