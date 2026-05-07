package com.erp.modules.procurement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierCreateRequest {

    @NotBlank(message = "Tedarikçi adı zorunludur.")
    private String name;

    @NotBlank(message = "İletişim e-postası zorunludur.")
    @Email(message = "Geçerli bir e-posta adresi girin.")
    private String contactEmail;

    @NotBlank(message = "Adres bilgisi zorunludur.")
    private String address;
}