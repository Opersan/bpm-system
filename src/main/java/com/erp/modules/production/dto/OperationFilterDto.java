package com.erp.modules.production.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OperationFilterDto {

    private String search;

    private Boolean active;

    public static OperationFilterDto empty() {
        return OperationFilterDto.builder().build();
    }
}
