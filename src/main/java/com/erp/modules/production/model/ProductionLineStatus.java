package com.erp.modules.production.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductionLineStatus {
    AVAILABLE("Uygun", "success"),
    BUSY("Meşgul", "warning"),
    MAINTENANCE("Bakımda", "danger"),
    OFFLINE("Devre Dışı", "neutral");

    private final String label;
    private final String cssClass;
}