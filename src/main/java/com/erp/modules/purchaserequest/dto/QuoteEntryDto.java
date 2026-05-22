package com.erp.modules.purchaserequest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class QuoteEntryDto {

    private List<QuoteLineDto> quotes = new ArrayList<>();

    @Data
    public static class QuoteLineDto {
        private Long supplierId;
        private BigDecimal totalAmount;
        private String currency;
        private LocalDate estimatedDeliveryDate;
        private String notes;
    }
}
