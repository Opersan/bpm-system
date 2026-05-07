package com.erp.modules.usermanagement.web;

import com.erp.modules.usermanagement.service.AuthorizationService;
import com.erp.modules.usermanagement.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalViewModelAdvice {

    private final AuthorizationService authorizationService;
    private final UserService userService;

    @ModelAttribute("currentUser")
    public Object currentUser() {
        return authorizationService.getCurrentUser();
    }

    @ModelAttribute("currentUserRole")
    public Object currentUserRole() {
        return authorizationService.getCurrentUser().getRole();
    }

    @ModelAttribute("demoUsers")
    public Object demoUsers() {
        return userService.listUsers();
    }

    @ModelAttribute("visibleNavigationItems")
    public Object visibleNavigationItems(HttpServletRequest request) {
        return authorizationService.getVisibleNavigationItems(request.getRequestURI());
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }
}