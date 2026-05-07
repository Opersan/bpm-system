package com.erp.modules.planning.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MrpStatus {
    ACIL("Acil", "danger"),
    KRITIK("Kritik", "warning"),
    PLANLANMALI("Planlanmalı", "primary"),
    NORMAL("Normal", "success");

    private final String label;
    private final String cssClass;
}
