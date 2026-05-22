package com.erp.modules.purchaserequest.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PurchaseRequestStatus {
    SUBMITTED("Onay Bekliyor", "warning"),
    QUOTES_ENTERED("Teklif Girildi", "info"),
    APPROVED("Onaylandı", "success"),
    REJECTED("Reddedildi", "danger");

    private final String label;
    private final String cssClass;
}
