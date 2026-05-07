package com.erp.modules.usermanagement.service;

import com.erp.modules.usermanagement.dto.NavigationItemDto;
import com.erp.modules.usermanagement.dto.NavigationLinkDto;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.List;
import java.util.Set;

@Service("authorizationService")
@RequiredArgsConstructor
public class AuthorizationService {

    private static final Set<UserRole> ALL_ROLES = Set.of(UserRole.values());

    private final UserService userService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public UserDto getCurrentUser() {
        return userService.getCurrentUser();
    }

    public boolean hasRole(UserRole role) {
        return getCurrentUser().getRole() == role;
    }

    public boolean hasAnyRole(UserRole... roles) {
        UserRole currentRole = getCurrentUser().getRole();
        for (UserRole role : roles) {
            if (role == currentRole) {
                return true;
            }
        }
        return false;
    }

    public void requireRole(UserRole role) {
        if (!hasRole(role)) {
            throw new IllegalStateException("Bu işlem için gerekli role sahip değilsiniz.");
        }
    }

    public void requireAnyRole(UserRole... roles) {
        if (!hasAnyRole(roles)) {
            throw new IllegalStateException("Bu sayfaya erişmek için gerekli role sahip değilsiniz.");
        }
    }

    public boolean canAccessPath(String path) {
        return canAccessPath(path, "GET");
    }

    public boolean canAccessPath(String path, String httpMethod) {
        UserRole currentRole = getCurrentUser().getRole();
        return resolveAllowedRoles(path, httpMethod).contains(currentRole);
    }

    public boolean canAccessUserManagement() {
        return hasRole(UserRole.ADMIN);
    }

    public boolean canCreatePurchaseOrder() {
        return hasAnyRole(UserRole.ADMIN, UserRole.PURCHASING_USER);
    }

    public boolean canCreateSupplier() {
        return hasAnyRole(UserRole.ADMIN, UserRole.PURCHASING_USER);
    }

    public boolean canCreatePlanningItem() {
        return hasAnyRole(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER);
    }

    public boolean canCreateWorkOrder() {
        return hasAnyRole(UserRole.ADMIN, UserRole.PRODUCTION_USER);
    }

    public boolean canManageWorkOrders() {
        return hasAnyRole(UserRole.ADMIN, UserRole.PRODUCTION_USER);
    }

    public List<NavigationItemDto> getVisibleNavigationItems(String currentPath) {
        List<NavigationItemDto> items = List.of(
            directItem("dashboard", "Dashboard", "dashboard", "/dashboard", currentPath, List.of("/", "/dashboard")),
            groupedItem("procurement", "Satın Alma", "procurement", currentPath, List.of(
                link("Yeni Satın Alma Siparişi", "/purchasing/new", currentPath, List.of("/purchasing/new", "/procurement/new")),
                link("Yeni Tedarikçi", "/purchasing/suppliers/new", currentPath, List.of("/purchasing/suppliers/new")),
                link("Satın Alma Siparişleri", "/purchasing/orders", currentPath, List.of("/purchasing/orders", "/procurement"))
            )),
            groupedItem("planning", "Planlama", "planning", currentPath, List.of(
                link("Envanter", "/planning/inventory", currentPath, List.of("/planning/inventory", "/inventory")),
                link("Yeni Kalem Oluştur", "/planning/items/new", currentPath, List.of("/planning/items/new")),
                link("MRP", "/planning/mrp", currentPath, List.of("/planning/mrp", "/mrp"))
            )),
            groupedItem("production", "Üretim", "production", currentPath, List.of(
                link("İş Emri Oluştur", "/production/work-orders/new", currentPath, List.of("/production/work-orders/new", "/manufacturing/new")),
                link("İş Emirleri", "/production/work-orders", currentPath, List.of("/production/work-orders", "/manufacturing"))
            )),
            groupedItem("admin", "Yönetim", "admin", currentPath, List.of(
                link("Kullanıcı Yönetimi", "/admin/users", currentPath, List.of("/admin/users", "/admin/users/new"))
            ))
        );

        return items.stream()
            .filter(item -> item.getUrl() != null || item.hasChildren())
            .toList();
    }

    private NavigationItemDto directItem(String key, String label, String iconKey, String url,
                                         String currentPath, List<String> activePatterns) {
        if (!canAccessPath(url)) {
            return NavigationItemDto.builder()
                .key(key)
                .label(label)
                .iconKey(iconKey)
                .children(List.of())
                .build();
        }
        boolean active = matchesAnyPattern(currentPath, activePatterns);
        return NavigationItemDto.builder()
            .key(key)
            .label(label)
            .iconKey(iconKey)
            .url(url)
            .active(active)
            .expanded(active)
            .children(List.of())
            .build();
    }

    private NavigationItemDto groupedItem(String key, String label, String iconKey, String currentPath,
                                          List<NavigationLinkDto> children) {
        List<NavigationLinkDto> visibleChildren = children.stream()
            .filter(child -> child.getUrl() != null)
            .toList();
        if (visibleChildren.isEmpty()) {
            return NavigationItemDto.builder()
                .key(key)
                .label(label)
                .iconKey(iconKey)
                .children(List.of())
                .build();
        }
        boolean expanded = visibleChildren.stream().anyMatch(NavigationLinkDto::isActive);
        return NavigationItemDto.builder()
            .key(key)
            .label(label)
            .iconKey(iconKey)
            .active(expanded)
            .expanded(expanded)
            .children(visibleChildren)
            .build();
    }

    private NavigationLinkDto link(String label, String url, String currentPath, List<String> activePatterns) {
        if (!canAccessPath(url)) {
            return NavigationLinkDto.builder().label(label).build();
        }
        return NavigationLinkDto.builder()
            .label(label)
            .url(url)
            .active(matchesAnyPattern(currentPath, activePatterns))
            .build();
    }

    private boolean matchesAnyPattern(String currentPath, List<String> patterns) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, currentPath));
    }

    private Set<UserRole> resolveAllowedRoles(String path, String httpMethod) {
        if (path == null || path.isBlank()) {
            return ALL_ROLES;
        }
        for (RouteRule rule : routeRules()) {
            if (rule.matches(path, httpMethod, pathMatcher)) {
                return rule.allowedRoles();
            }
        }
        return ALL_ROLES;
    }

    private List<RouteRule> routeRules() {
        return List.of(
            new RouteRule("/", Set.of("GET"), ALL_ROLES),
            new RouteRule("/dashboard", Set.of("GET"), ALL_ROLES),
            new RouteRule("/unauthorized", null, ALL_ROLES),
            new RouteRule("/auth/switch-user", Set.of("GET"), ALL_ROLES),
            new RouteRule("/admin/**", null, Set.of(UserRole.ADMIN)),
            new RouteRule("/purchasing/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/purchasing/suppliers/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/purchasing/suppliers", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/purchasing/orders", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PURCHASING_USER)),
            new RouteRule("/purchasing/orders", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/purchasing/orders/**", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/procurement/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.PURCHASING_USER)),
            new RouteRule("/procurement", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PURCHASING_USER)),
            new RouteRule("/planning/items/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER)),
            new RouteRule("/planning/items", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER)),
            new RouteRule("/planning/**", null, Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER)),
            new RouteRule("/inventory/**", null, Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER)),
            new RouteRule("/mrp/**", null, Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PLANNING_USER)),
            new RouteRule("/production/work-orders/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/production/work-orders", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PRODUCTION_USER, UserRole.OPERATOR)),
            new RouteRule("/production/work-orders", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/production/work-orders/*/start", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/production/work-orders/*/pause", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/production/work-orders/*/complete", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/production/work-orders/*/cancel", Set.of("POST"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/manufacturing/new", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.PRODUCTION_USER)),
            new RouteRule("/manufacturing", Set.of("GET"), Set.of(UserRole.ADMIN, UserRole.MANAGER, UserRole.PRODUCTION_USER, UserRole.OPERATOR))
        );
    }

    private record RouteRule(String pattern, Set<String> methods, Set<UserRole> allowedRoles) {
        private boolean matches(String path, String method, AntPathMatcher matcher) {
            boolean methodMatches = methods == null || methods.isEmpty() || methods.contains(method);
            return methodMatches && matcher.match(pattern, path);
        }
    }
}