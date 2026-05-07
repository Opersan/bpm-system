package com.erp.modules.planning.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InventoryStatus {
    YETERLI("Yeterli", "success"),
    DUSUK("Düşük", "warning"),
    KRITIK("Kritik", "danger"),
    STOK_YOK("Stok Yok", "danger"),
    FAZLA_STOK("Fazla Stok", "info");

    private final String label;
    private final String cssClass;
}
