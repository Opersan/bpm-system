package com.erp.modules.planning.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MrpActionType {
    SATIN_ALMA_TALEBI_OLUSTUR("Satın Alma Talebi Oluştur", "primary"),
    URETIM_EMRI_OLUSTUR("Üretim Emri Oluştur", "info"),
    TRANSFER_ONER("Depolar Arası Transfer Öner", "warning"),
    AKSIYON_YOK("Aksiyon Yok", "secondary");

    private final String label;
    private final String cssClass;
}
