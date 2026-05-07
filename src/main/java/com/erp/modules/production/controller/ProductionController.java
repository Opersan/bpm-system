package com.erp.modules.production.controller;

import com.erp.modules.production.dto.WorkOrderCreateRequest;
import com.erp.modules.production.dto.WorkOrderDto;
import com.erp.modules.production.dto.WorkOrderFilterDto;
import com.erp.modules.production.service.ProductionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;

    @GetMapping("/work-orders")
    public String workOrders(@RequestParam(required = false) String search,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String priority,
                             @RequestParam(required = false) String line,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                             @RequestParam(defaultValue = "false") boolean delayedOnly,
                             Model model) {
        WorkOrderFilterDto filters = productionService.createFilter(search, status, priority, line, startDate, endDate, delayedOnly);
        List<WorkOrderDto> workOrders = productionService.getFilteredWorkOrders(filters);

        model.addAttribute("pageTitle", "İş Emirleri");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "İş Emirleri"));
        model.addAttribute("workOrders", workOrders);
        model.addAttribute("statuses", productionService.getStatuses());
        model.addAttribute("priorities", productionService.getPriorities());
        model.addAttribute("productionLines", productionService.getProductionLines());
        model.addAttribute("filters", filters);
        model.addAttribute("summaryCards", productionService.getSummaryCards(workOrders));
        return "production/work-orders";
    }

    @GetMapping("/work-orders/new")
    public String newWorkOrder(Model model) {
        populateFormModel(model, productionService.defaultCreateRequest());
        return "production/work-order-new";
    }

    @PostMapping("/work-orders")
    public String createWorkOrder(@Valid @ModelAttribute("workOrderRequest") WorkOrderCreateRequest workOrderRequest,
                                  BindingResult bindingResult,
                                  @RequestParam(defaultValue = "PLANNED") String submitAction,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        validateDateRange(workOrderRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları ve tarih aralığını kontrol edin.");
            populateFormModel(model, workOrderRequest);
            return "production/work-order-new";
        }

        boolean draft = "DRAFT".equalsIgnoreCase(submitAction) || "draft".equalsIgnoreCase(submitAction);
        WorkOrderDto workOrder = productionService.createWorkOrder(workOrderRequest, draft);
        redirectAttributes.addFlashAttribute("successMessage",
            draft ? workOrder.getWorkOrderNo() + " taslak olarak kaydedildi." : workOrder.getWorkOrderNo() + " planlandı.");
        return "redirect:/production/work-orders";
    }

    @PostMapping("/work-orders/{workOrderNo}/start")
    public String startWorkOrder(@PathVariable String workOrderNo, RedirectAttributes redirectAttributes) {
        return handleStatusChange(() -> productionService.startWorkOrder(workOrderNo), redirectAttributes);
    }

    @PostMapping("/work-orders/{workOrderNo}/pause")
    public String pauseWorkOrder(@PathVariable String workOrderNo, RedirectAttributes redirectAttributes) {
        return handleStatusChange(() -> productionService.pauseWorkOrder(workOrderNo), redirectAttributes);
    }

    @PostMapping("/work-orders/{workOrderNo}/complete")
    public String completeWorkOrder(@PathVariable String workOrderNo, RedirectAttributes redirectAttributes) {
        return handleStatusChange(() -> productionService.completeWorkOrder(workOrderNo), redirectAttributes);
    }

    @PostMapping("/work-orders/{workOrderNo}/cancel")
    public String cancelWorkOrder(@PathVariable String workOrderNo, RedirectAttributes redirectAttributes) {
        return handleStatusChange(() -> productionService.cancelWorkOrder(workOrderNo), redirectAttributes);
    }

    private void populateFormModel(Model model, WorkOrderCreateRequest request) {
        WorkOrderCreateRequest preparedRequest = productionService.prepareCreateRequest(request);
        model.addAttribute("pageTitle", "İş Emri Oluştur");
        model.addAttribute("activePage", "production");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Üretim", "İş Emri Oluştur"));
        model.addAttribute("workOrderRequest", preparedRequest);
        model.addAttribute("products", productionService.getProducts());
        model.addAttribute("productionLines", productionService.getProductionLines());
        model.addAttribute("priorities", productionService.getPriorities());
        model.addAttribute("shifts", productionService.getShifts());
        model.addAttribute("responsibleUsers", productionService.getResponsibleUsers());
        model.addAttribute("selectedProduct", productionService.getSelectedProduct(preparedRequest.getProductCode()));
        model.addAttribute("selectedProductionLine", productionService.getSelectedProductionLine(preparedRequest.getProductionLineCode()));
        model.addAttribute("materialRequirements", productionService.calculateMaterialRequirements(preparedRequest.getProductCode(), preparedRequest.getPlannedQuantity()));
        model.addAttribute("materialTemplates", productionService.getMaterialTemplates());
        model.addAttribute("summary", productionService.createFormSummary(preparedRequest));
    }

    private void validateDateRange(WorkOrderCreateRequest request, BindingResult bindingResult) {
        if (request.getPlannedStartDate() != null
            && request.getPlannedEndDate() != null
            && request.getPlannedEndDate().isBefore(request.getPlannedStartDate())) {
            bindingResult.addError(new FieldError("workOrderRequest", "plannedEndDate", "Bitiş tarihi başlangıç tarihinden önce olamaz."));
        }
        if (request.getPlannedQuantity() != null && request.getPlannedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            bindingResult.addError(new FieldError("workOrderRequest", "plannedQuantity", "Planlanan miktar 0'dan büyük olmalıdır."));
        }
    }

    private String handleStatusChange(TransitionOperation operation, RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", operation.execute());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/production/work-orders";
    }

    @FunctionalInterface
    private interface TransitionOperation {
        String execute();
    }
}