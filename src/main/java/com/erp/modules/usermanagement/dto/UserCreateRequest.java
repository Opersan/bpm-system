package com.erp.modules.usermanagement.dto;

import com.erp.modules.usermanagement.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    @NotBlank(message = "Kullanıcı adı zorunludur.")
    private String username;

    @NotBlank(message = "Ad soyad zorunludur.")
    private String fullName;

    @NotBlank(message = "E-posta zorunludur.")
    @Email(message = "Geçerli bir e-posta adresi girin.")
    private String email;

    @NotNull(message = "Rol seçimi zorunludur.")
    private UserRole role;

    @Default
    private boolean active = true;
}