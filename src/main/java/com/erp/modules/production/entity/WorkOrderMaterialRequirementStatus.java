package com.erp.modules.production.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderMaterialRequirementStatus {
    PLANNED("Planlandı", "info"),
    RESERVED("Rezerve Edildi", "primary"),
    ISSUED("Çıkarıldı", "success"),
    CANCELLED("İptal", "danger");

    private final String label;
    private final String cssClass;
}
