package com.erp.modules.production.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkOrderPriority {
    LOW("Düşük", "neutral", 1),
    NORMAL("Normal", "info", 2),
    HIGH("Yüksek", "warning", 3),
    CRITICAL("Kritik", "danger", 4);

    private final String label;
    private final String cssClass;
    private final int sortOrder;
}