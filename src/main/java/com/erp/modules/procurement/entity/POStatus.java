package com.erp.modules.procurement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum POStatus {
    DRAFT("Taslak", "secondary"),
    PENDING_APPROVAL("Onay Bekliyor", "warning"),
    APPROVED("Onaylandı", "primary"),
    SENT("Gönderildi", "info"),
    PARTIALLY_RECEIVED("Kısmi Teslim", "info"),
    CLOSED("Tamamlandı", "success"),
    CANCELLED("İptal", "danger");

    private final String label;
    private final String cssClass;
}
