package com.erp.modules.planning.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ItemCreateRequest {

    @NotBlank(message = "Malzeme kodu zorunludur.")
    private String code;

    @NotBlank(message = "Malzeme adı zorunludur.")
    private String name;

    private String description;

    @NotNull(message = "Birim fiyat zorunludur.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Birim fiyat negatif olamaz.")
    private BigDecimal price;

    @NotBlank(message = "Birim seçimi zorunludur.")
    private String uom;
}