package com.erp.modules.usermanagement.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    ADMIN("ADMIN", "Admin", "danger"),
    MANAGER("MANAGER", "Yönetici", "primary"),
    PURCHASING_USER("PURCHASING_USER", "Satın Alma", "info"),
    PLANNING_USER("PLANNING_USER", "Planlama", "warning"),
    PRODUCTION_USER("PRODUCTION_USER", "Üretim", "success"),
    PRODUCTION_SUPERVISOR("PRODUCTION_SUPERVISOR", "Üretim Sorumlusu", "primary"),
    OPERATOR("OPERATOR", "Operatör", "neutral"),
    WAREHOUSE_USER("WAREHOUSE_USER", "Ambar Sorumlusu", "neutral");

    private final String code;
    private final String label;
    private final String cssClass;
}