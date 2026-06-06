package com.erp.modules.usermanagement.web;

import com.erp.modules.usermanagement.service.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthorizationInterceptor implements HandlerInterceptor {

    private final AuthorizationService authorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authorization for /api/** endpoints (handled by Spring Security which has permitAll)
        if (uri.startsWith("/api/")) {
            return true;
        }
        
        boolean allowed = authorizationService.canAccessPath(uri, method);
        
        System.out.println("Authorization: " + method + " " + uri + " = " + (allowed ? "ALLOWED" : "DENIED"));
        
        if (allowed) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + "/unauthorized");
        return false;
    }
}