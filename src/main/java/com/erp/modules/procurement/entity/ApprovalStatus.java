package com.erp.modules.procurement.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApprovalStatus {
    NOT_SUBMITTED("Gönderilmedi", "secondary"),
    WAITING("Bekliyor", "warning"),
    APPROVED("Onaylandı", "success"),
    REJECTED("Reddedildi", "danger");

    private final String label;
    private final String cssClass;
}
