package com.erp.modules.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class OperationMaterialRequirementDto {

    private Long id;

    @NotNull(message = "Malzeme seçimi zorunludur.")
    private String itemId;

    @NotBlank(message = "Malzeme adı zorunludur.")
    private String itemName;

    @NotBlank(message = "Birim zorunludur.")
    private String unit;

    @NotNull(message = "Gerekli miktar zorunludur.")
    private BigDecimal requiredQuantity = BigDecimal.ONE;

    private BigDecimal scrapRate = BigDecimal.ZERO;

    private boolean critical = false;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
    private String description;
}
