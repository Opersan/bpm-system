package com.erp.modules.production.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MaterialRequirementStatus {
    AVAILABLE("Yeterli", "success"),
    LOW_STOCK("Düşük Stok", "warning"),
    MISSING("Eksik", "danger"),
    CRITICAL("Kritik", "danger");

    private final String label;
    private final String cssClass;
}