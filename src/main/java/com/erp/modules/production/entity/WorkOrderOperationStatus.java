package com.erp.modules.production.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderOperationStatus {
    PENDING("Bekliyor", "neutral"),
    IN_PROGRESS("Devam Ediyor", "primary"),
    COMPLETED("Tamamlandı", "success"),
    CANCELLED("İptal Edildi", "danger");

    private final String label;
    private final String cssClass;
}
