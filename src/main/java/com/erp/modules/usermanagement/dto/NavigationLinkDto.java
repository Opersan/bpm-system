package com.erp.modules.usermanagement.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NavigationLinkDto {
    private final String label;
    private final String url;
    private final boolean active;
}