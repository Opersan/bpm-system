package com.erp.modules.production.dto;

import com.erp.modules.production.model.WorkOrderPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreateRequest {
    @NotBlank(message = "Ürün seçimi zorunludur.")
    private String productCode;

    @NotNull(message = "Planlanan miktar zorunludur.")
    @DecimalMin(value = "0.01", message = "Planlanan miktar 0'dan büyük olmalıdır.")
    private BigDecimal plannedQuantity;

    private String unit;

    @NotNull(message = "Öncelik seçimi zorunludur.")
    private WorkOrderPriority priority;

    @NotBlank(message = "Üretim hattı zorunludur.")
    private String productionLineCode;

    @NotBlank(message = "Sorumlu bilgisi zorunludur.")
    private String responsible;

    @NotNull(message = "Planlanan başlangıç tarihi zorunludur.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedStartDate;

    @NotNull(message = "Planlanan bitiş tarihi zorunludur.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedEndDate;

    @NotBlank(message = "Vardiya seçimi zorunludur.")
    private String shift;

    @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir.")
    private String description;
}