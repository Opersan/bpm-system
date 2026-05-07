package com.erp.modules.production.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderStatus {
    DRAFT("Taslak", "neutral"),
    PLANNED("Planlandı", "info"),
    IN_PROGRESS("Devam Ediyor", "primary"),
    PAUSED("Durduruldu", "warning"),
    COMPLETED("Tamamlandı", "success"),
    CANCELLED("İptal", "danger"),
    DELAYED("Gecikti", "danger");

    private final String label;
    private final String cssClass;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}