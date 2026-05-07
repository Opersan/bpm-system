package com.erp.modules.usermanagement.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NavigationItemDto {
    private final String key;
    private final String label;
    private final String iconKey;
    private final String url;
    private final boolean active;
    private final boolean expanded;
    private final List<NavigationLinkDto> children;

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}