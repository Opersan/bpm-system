package com.erp.modules.planning.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MrpActionType {
    SATIN_ALMA_OLUSTUR("Satın Alma Siparişi Oluştur", "primary"),
    URETIM_EMRI_OLUSTUR("Üretim Emri Oluştur", "info"),
    TRANSFER_ONER("Depolar Arası Transfer", "warning"),
    AKSIYON_YOK("Aksiyon Yok", "secondary");

    private final String label;
    private final String cssClass;
}
