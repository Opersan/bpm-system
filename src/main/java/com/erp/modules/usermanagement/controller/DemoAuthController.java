package com.erp.modules.usermanagement.controller;

import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class DemoAuthController {

    private final UserService userService;

    @GetMapping("/switch-user")
    public String switchUser(@RequestParam String username,
                             @RequestParam(required = false) String redirect,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            UserDto switchedUser = userService.switchUser(username, session);
            redirectAttributes.addFlashAttribute("successMessage", switchedUser.getFullName() + " kullanıcısına geçildi.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + sanitizeRedirect(redirect);
    }

    private String sanitizeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/dashboard";
        }
        return redirect;
    }
}