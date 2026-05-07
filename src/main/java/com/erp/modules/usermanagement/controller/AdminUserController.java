package com.erp.modules.usermanagement.controller;

import com.erp.modules.usermanagement.dto.UserCreateRequest;
import com.erp.modules.usermanagement.model.UserRole;
import com.erp.modules.usermanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public String users(Model model) {
        model.addAttribute("pageTitle", "Kullanıcı Yönetimi");
        model.addAttribute("activePage", "admin");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Yönetim", "Kullanıcılar"));
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("roles", userService.getRoles());
        return "admin/users";
    }

    @GetMapping("/new")
    public String newUser(Model model) {
        populateFormModel(model, new UserCreateRequest());
        return "admin/user-new";
    }

    @PostMapping
    public String createUser(@Valid @ModelAttribute("userCreateRequest") UserCreateRequest userCreateRequest,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları kontrol edin.");
            populateFormModel(model, userCreateRequest);
            return "admin/user-new";
        }

        try {
            userService.createUser(userCreateRequest);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("username", "duplicate", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, userCreateRequest);
            return "admin/user-new";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Yeni kullanıcı başarıyla oluşturuldu.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean active = userService.toggleActive(id, userService.getCurrentUser()).isActive();
            redirectAttributes.addFlashAttribute("successMessage", active ? "Kullanıcı aktif hale getirildi." : "Kullanıcı pasif hale getirildi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/change-role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam UserRole role,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.changeRole(id, role);
            redirectAttributes.addFlashAttribute("successMessage", "Kullanıcı rolü güncellendi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    private void populateFormModel(Model model, UserCreateRequest request) {
        model.addAttribute("pageTitle", "Yeni Kullanıcı Oluştur");
        model.addAttribute("activePage", "admin");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Yönetim", "Kullanıcılar", "Yeni Kullanıcı"));
        model.addAttribute("userCreateRequest", request);
        model.addAttribute("roles", userService.getRoles());
    }
}