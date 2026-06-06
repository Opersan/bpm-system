package com.erp.modules.production.dto;

import jakarta.validation.constraints.*;
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
public class OperationCreateRequest {

    @NotBlank(message = "Operasyon kodu zorunludur.")
    @Size(min = 1, max = 50, message = "Operasyon kodu 1-50 karakter arasında olmalıdır.")
    private String code;

    @NotBlank(message = "Operasyon adı zorunludur.")
    @Size(min = 1, max = 200, message = "Operasyon adı 1-200 karakter arasında olmalıdır.")
    private String name;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir.")
    private String description;

    @NotNull(message = "Standart süre zorunludur.")
    @DecimalMin(value = "0", inclusive = false, message = "Standart süre 0'dan büyük olmalıdır.")
    private BigDecimal standardDuration = BigDecimal.ONE;

    @NotBlank(message = "Süre birimi zorunludur.")
    private String durationUnit = "MINUTES";

    @NotNull(message = "Varsayılan sıra numarası zorunludur.")
    @Min(value = 1, message = "Varsayılan sıra numarası en az 1 olmalıdır.")
    private Integer defaultSequence = 1;

    @Size(max = 100, message = "İş merkezi 100 karakter olmalıdır.")
    private String workCenter;

    @DecimalMin(value = "0", message = "Kapasite negatif olamaz.")
    private BigDecimal capacity = BigDecimal.ZERO;

    @Size(max = 50, message = "Kapasite birimi 50 karakter olmalıdır.")
    private String capacityUnit;

    private boolean active = true;

    @Size(max = 50, message = "Operasyon malzeme ihtiyaçları en fazla 50 adet olabilir.")
    private List<OperationMaterialRequirementDto> materialRequirements = new ArrayList<>();
}
