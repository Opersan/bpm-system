package com.erp.modules.usermanagement.service;

import com.erp.modules.usermanagement.dto.UserCreateRequest;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.model.UserRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {

    private static final String CURRENT_USER_SESSION_KEY = "demoCurrentUser";
    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    private final Map<Long, UserDto> usersById = new ConcurrentHashMap<>();
    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public UserService() {
        seedUsers();
    }

    public List<UserDto> listUsers() {
        return usersById.values().stream()
            .map(this::copy)
            .sorted(Comparator.comparing(UserDto::getCreatedAt).thenComparing(UserDto::getId))
            .toList();
    }

    public List<UserRole> getRoles() {
        return List.of(UserRole.values());
    }

    public Optional<UserDto> findByUsername(String username) {
        String normalized = normalizeUsername(username);
        if (normalized == null || !usernameIndex.containsKey(normalized)) {
            return Optional.empty();
        }
        return findById(usernameIndex.get(normalized));
    }

    public Optional<UserDto> findById(Long id) {
        return Optional.ofNullable(usersById.get(id)).map(this::copy);
    }

    public UserDto createUser(UserCreateRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (username == null) {
            throw new IllegalArgumentException("Kullanıcı adı zorunludur.");
        }
        if (usernameIndex.containsKey(username)) {
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kayıtlı.");
        }

        UserDto user = UserDto.builder()
            .id(sequence.incrementAndGet())
            .username(username)
            .fullName(trimToNull(request.getFullName()))
            .email(trimToNull(request.getEmail()))
            .role(request.getRole())
            .active(request.isActive())
            .createdAt(LocalDateTime.now())
            .build();

        usersById.put(user.getId(), user);
        usernameIndex.put(username, user.getId());
        return copy(user);
    }

    public UserDto toggleActive(Long id, UserDto actor) {
        UserDto stored = requireStoredUser(id);
        if (actor != null && actor.getId().equals(stored.getId()) && stored.isActive()) {
            throw new IllegalStateException("Aktif kullanıcı kendi hesabını pasif yapamaz.");
        }
        if (stored.getRole() == UserRole.ADMIN && stored.isActive() && countActiveAdmins() == 1) {
            throw new IllegalStateException("Son aktif admin kullanıcı pasif yapılamaz.");
        }
        stored.setActive(!stored.isActive());
        return copy(stored);
    }

    public UserDto changeRole(Long id, UserRole newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Geçerli bir rol seçiniz.");
        }
        UserDto stored = requireStoredUser(id);
        if (stored.getRole() == UserRole.ADMIN && stored.isActive() && countActiveAdmins() == 1 && newRole != UserRole.ADMIN) {
            throw new IllegalStateException("Son aktif admin kullanıcının rolü değiştirilemez.");
        }
        stored.setRole(newRole);
        return copy(stored);
    }

    public boolean canToggleActive(UserDto targetUser, UserDto currentUser) {
        if (targetUser == null) {
            return false;
        }
        if (currentUser != null && targetUser.getId().equals(currentUser.getId()) && targetUser.isActive()) {
            return false;
        }
        return !(targetUser.getRole() == UserRole.ADMIN && targetUser.isActive() && countActiveAdmins() == 1);
    }

    public boolean canChangeRole(UserDto targetUser) {
        if (targetUser == null) {
            return false;
        }
        return !(targetUser.getRole() == UserRole.ADMIN && targetUser.isActive() && countActiveAdmins() == 1);
    }

    public UserDto getCurrentUser() {
        return getCurrentUser(resolveSession(true));
    }

    public UserDto getCurrentUser(HttpSession session) {
        String username = session != null ? (String) session.getAttribute(CURRENT_USER_SESSION_KEY) : null;
        Optional<UserDto> selectedUser = findByUsername(username).filter(UserDto::isActive);
        UserDto currentUser = selectedUser.or(() -> findByUsername("admin")).orElseThrow();
        if (session != null) {
            session.setAttribute(CURRENT_USER_SESSION_KEY, currentUser.getUsername());
        }
        return currentUser;
    }

    public UserDto switchUser(String username) {
        return switchUser(username, resolveSession(true));
    }

    public UserDto switchUser(String username, HttpSession session) {
        UserDto user = findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + username));
        if (!user.isActive()) {
            throw new IllegalStateException("Pasif kullanıcıya geçiş yapılamaz.");
        }
        if (session != null) {
            session.setAttribute(CURRENT_USER_SESSION_KEY, user.getUsername());
        }
        return user;
    }

    private void seedUsers() {
        addSeedUser("admin", "Sistem Yöneticisi", "admin@erp.com", UserRole.ADMIN, true, LocalDateTime.now().minusDays(120));
        addSeedUser("manager", "Operasyon Müdürü", "manager@erp.com", UserRole.MANAGER, true, LocalDateTime.now().minusDays(90));
        addSeedUser("purchasing", "Satın Alma Sorumlusu", "purchasing@erp.com", UserRole.PURCHASING_USER, true, LocalDateTime.now().minusDays(70));
        addSeedUser("planning", "Planlama Uzmanı", "planning@erp.com", UserRole.PLANNING_USER, true, LocalDateTime.now().minusDays(60));
        addSeedUser("production", "Üretim Sorumlusu", "production@erp.com", UserRole.PRODUCTION_USER, true, LocalDateTime.now().minusDays(55));
        addSeedUser("operator", "Hat Operatörü", "operator@erp.com", UserRole.OPERATOR, true, LocalDateTime.now().minusDays(40));
        addSeedUser("warehouse", "Ambar Sorumlusu", "warehouse@erp.com", UserRole.WAREHOUSE_USER, true, LocalDateTime.now().minusDays(50));
    }

    private void addSeedUser(String username, String fullName, String email, UserRole role, boolean active, LocalDateTime createdAt) {
        Long id = sequence.incrementAndGet();
        UserDto user = UserDto.builder()
            .id(id)
            .username(username)
            .fullName(fullName)
            .email(email)
            .role(role)
            .active(active)
            .createdAt(createdAt)
            .build();
        usersById.put(id, user);
        usernameIndex.put(username, id);
    }

    private UserDto requireStoredUser(Long id) {
        UserDto user = usersById.get(id);
        if (user == null) {
            throw new IllegalArgumentException("Kullanıcı bulunamadı: " + id);
        }
        return user;
    }

    private long countActiveAdmins() {
        return usersById.values().stream()
            .filter(UserDto::isActive)
            .filter(UserDto::isAdmin)
            .count();
    }

    private String normalizeUsername(String username) {
        String value = trimToNull(username);
        return value != null ? value.toLowerCase(TR_LOCALE) : null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private HttpSession resolveSession(boolean create) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest().getSession(create);
    }

    private UserDto copy(UserDto user) {
        return user.toBuilder().build();
    }
}