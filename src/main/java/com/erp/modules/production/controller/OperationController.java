package com.erp.modules.production.controller;

import com.erp.modules.production.dto.OperationCreateRequest;
import com.erp.modules.production.dto.OperationDto;
import com.erp.modules.production.dto.OperationFilterDto;
import com.erp.modules.production.service.OperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/production/operations")
@RequiredArgsConstructor
public class OperationController {

    private final OperationService operationService;

    @GetMapping
    public String listOperations(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) String active,
                                 Model model) {
        OperationFilterDto filter = OperationFilterDto.builder()
            .search(search)
            .active("true".equals(active) ? true : ("false".equals(active) ? false : null))
            .build();
        // Ensure filter is never null for template access
        if (filter == null) {
            filter = OperationFilterDto.builder().build();
        }

        List<OperationDto> operations = operationService.getOperations(filter);
        model.addAttribute("operations", operations);
        model.addAttribute("filter", filter);
        model.addAttribute("pageTitle", "Operasyonlar");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar"));
        return "production/operations";
    }

    @GetMapping("/new")
    public String newOperation(Model model) {
        OperationCreateRequest request = new OperationCreateRequest();
        request.setStandardDuration(java.math.BigDecimal.ONE);
        request.setDefaultSequence(1);
        request.setActive(true);
        model.addAttribute("operationRequest", request);
        model.addAttribute("pageTitle", "Yeni Operasyon");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar", "Yeni Operasyon"));
        return "production/operation-new";
    }

    @PostMapping
    public String createOperation(@Valid @ModelAttribute("operationRequest") OperationCreateRequest request,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları kontrol edin.");
            model.addAttribute("pageTitle", "Yeni Operasyon");
            model.addAttribute("activePage", "production");
            model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar", "Yeni Operasyon"));
            return "production/operation-new";
        }

        OperationDto operation = operationService.createOperation(request);
        redirectAttributes.addFlashAttribute("successMessage", operation.getName() + " operasyonu oluşturuldu.");
        return "redirect:/production/operations";
    }

    @GetMapping("/{id}")
    public String viewOperation(@PathVariable Long id, Model model) {
        OperationDto operation = operationService.getOperation(id);
        model.addAttribute("operation", operation);
        model.addAttribute("pageTitle", "Operasyon Detay");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar", operation.getName()));
        return "production/operation-view";
    }

    @GetMapping("/{id}/edit")
    public String editOperation(@PathVariable Long id, Model model) {
        OperationDto operation = operationService.getOperation(id);
        OperationCreateRequest request = OperationCreateRequest.builder()
            .code(operation.getCode())
            .name(operation.getName())
            .description(operation.getDescription())
            .standardDuration(operation.getStandardDuration())
            .durationUnit(operation.getDurationUnit())
            .defaultSequence(operation.getDefaultSequence())
            .workCenter(operation.getWorkCenter())
            .capacity(operation.getCapacity())
            .capacityUnit(operation.getCapacityUnit())
            .active(operation.isActive())
            .materialRequirements(operation.getMaterialRequirements())
            .build();
        model.addAttribute("operationRequest", request);
        model.addAttribute("pageTitle", "Operasyon Düzenle");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar", operation.getName(), "Düzenle"));
        return "production/operation-edit";
    }

    @PostMapping("/{id}")
    public String updateOperation(@PathVariable Long id,
                                  @Valid @ModelAttribute("operationRequest") OperationCreateRequest request,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları kontrol edin.");
            model.addAttribute("pageTitle", "Operasyon Düzenle");
            model.addAttribute("activePage", "production");
            model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "Operasyonlar", "Düzenle"));
            return "production/operation-edit";
        }

        OperationDto operation = operationService.updateOperation(id, request);
        redirectAttributes.addFlashAttribute("successMessage", operation.getName() + " operasyonu güncellendi.");
        return "redirect:/production/operations";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateOperation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            operationService.deactivateOperation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Operasyon pasif hale getirildi.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/production/operations";
    }

    @GetMapping("/active")
    @ResponseBody
    public List<OperationDto> getActiveOperations() {
        return operationService.getActiveOperations();
    }
}
