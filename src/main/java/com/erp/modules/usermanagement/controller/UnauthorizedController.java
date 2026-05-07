package com.erp.modules.usermanagement.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UnauthorizedController {

    @GetMapping("/unauthorized")
    public String unauthorized(Model model) {
        model.addAttribute("pageTitle", "Yetkisiz Erişim");
        model.addAttribute("activePage", "unauthorized");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Yetkisiz Erişim", "Erişim Reddedildi"));
        return "unauthorized";
    }
}