package com.erp.modules.planning.controller;

import com.erp.modules.planning.dto.InventoryFilterDto;
import com.erp.modules.planning.dto.InventoryItemDto;
import com.erp.modules.planning.dto.ItemCreateRequest;
import com.erp.modules.planning.dto.MrpRequestDto;
import com.erp.modules.planning.dto.MrpResultDto;
import com.erp.modules.planning.dto.SummaryCardDto;
import com.erp.modules.planning.service.PlanningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/planning")
@RequiredArgsConstructor
public class PlanningController {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PlanningService planningService;

    @GetMapping("/inventory")
    public String inventory(@RequestParam(required = false) String search,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String warehouse,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "false") boolean criticalOnly,
                            Model model) {
        InventoryFilterDto filters = planningService.createInventoryFilter(search, category, warehouse, status, criticalOnly);
        List<InventoryItemDto> inventoryItems = planningService.getInventoryItems(filters);

        model.addAttribute("pageTitle", "Envanter");
        model.addAttribute("activePage", "inventory");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Planlama", "Envanter"));
        model.addAttribute("inventoryItems", inventoryItems);
        model.addAttribute("categories", planningService.getCategories());
        model.addAttribute("warehouses", planningService.getWarehouses());
        model.addAttribute("statuses", planningService.getInventoryStatuses());
        model.addAttribute("filters", filters);
        model.addAttribute("summaryCards", planningService.getInventorySummaryCards(inventoryItems));
        model.addAttribute("warehouseSummaries", planningService.getWarehouseSummaries(inventoryItems));
        return "planning/inventory";
    }

    @GetMapping("/items/new")
    public String newItem(Model model) {
        populateItemFormModel(model, new ItemCreateRequest());
        return "planning/item-new";
    }

    @PostMapping("/items")
    public String createItem(@Valid @ModelAttribute("itemCreateRequest") ItemCreateRequest itemCreateRequest,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları kontrol edin.");
            populateItemFormModel(model, itemCreateRequest);
            return "planning/item-new";
        }

        try {
            planningService.createItem(itemCreateRequest);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("code", "duplicate", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            populateItemFormModel(model, itemCreateRequest);
            return "planning/item-new";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Yeni kalem oluşturuldu.");
        redirectAttributes.addAttribute("search", itemCreateRequest.getCode());
        return "redirect:/planning/inventory";
    }

    @GetMapping("/mrp")
    public String mrp(@RequestParam(required = false) String runId,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                      @RequestParam(required = false) String productGroup,
                      @RequestParam(required = false) String warehouse,
                      @RequestParam(required = false) Boolean includeSafetyStock,
                      @RequestParam(required = false) Boolean includeOpenPurchaseOrders,
                      @RequestParam(required = false) Boolean includeOpenWorkOrders,
                      Model model) {
        MrpRequestDto mrpRequest = createMrpRequest(startDate, endDate, productGroup, warehouse,
            includeSafetyStock, includeOpenPurchaseOrders, includeOpenWorkOrders);
        boolean hasRun = runId != null && !runId.isBlank();
        List<MrpResultDto> mrpResults = hasRun ? planningService.runMrp(mrpRequest) : List.of();
        List<SummaryCardDto> summaryCards = hasRun ? planningService.getMrpSummaryCards(mrpResults) : List.of();

        model.addAttribute("pageTitle", "MRP Planlama");
        model.addAttribute("activePage", "mrp");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Planlama", "MRP"));
        model.addAttribute("mrpRequest", mrpRequest);
        model.addAttribute("mrpResults", mrpResults);
        model.addAttribute("productGroups", planningService.getProductGroups());
        model.addAttribute("warehouses", planningService.getWarehouses());
        model.addAttribute("summaryCards", summaryCards);
        model.addAttribute("hasRun", hasRun);
        model.addAttribute("runId", runId);
        return "planning/mrp";
    }

    @PostMapping("/mrp/run")
    public String runMrp(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(required = false) String productGroup,
                         @RequestParam(required = false) String warehouse,
                         @RequestParam(defaultValue = "false") boolean includeSafetyStock,
                         @RequestParam(defaultValue = "false") boolean includeOpenPurchaseOrders,
                         @RequestParam(defaultValue = "false") boolean includeOpenWorkOrders,
                         RedirectAttributes redirectAttributes) {
        MrpRequestDto request = createMrpRequest(startDate, endDate, productGroup, warehouse,
            includeSafetyStock, includeOpenPurchaseOrders, includeOpenWorkOrders);
        request = planningService.normalizeMrpRequest(request);

        redirectAttributes.addAttribute("runId", planningService.createRunId());
        redirectAttributes.addAttribute("startDate", request.getStartDate().format(ISO_DATE));
        redirectAttributes.addAttribute("endDate", request.getEndDate().format(ISO_DATE));
        redirectAttributes.addAttribute("productGroup", request.getProductGroup());
        redirectAttributes.addAttribute("warehouse", request.getWarehouse());
        redirectAttributes.addAttribute("includeSafetyStock", request.isIncludeSafetyStock());
        redirectAttributes.addAttribute("includeOpenPurchaseOrders", request.isIncludeOpenPurchaseOrders());
        redirectAttributes.addAttribute("includeOpenWorkOrders", request.isIncludeOpenWorkOrders());
        return "redirect:/planning/mrp";
    }

    private MrpRequestDto createMrpRequest(LocalDate startDate, LocalDate endDate, String productGroup, String warehouse,
                                           Boolean includeSafetyStock, Boolean includeOpenPurchaseOrders,
                                           Boolean includeOpenWorkOrders) {
        MrpRequestDto request = planningService.defaultMrpRequest();
        if (startDate != null) {
            request.setStartDate(startDate);
        }
        if (endDate != null) {
            request.setEndDate(endDate);
        }
        if (productGroup != null && !productGroup.isBlank()) {
            request.setProductGroup(productGroup);
        }
        if (warehouse != null && !warehouse.isBlank()) {
            request.setWarehouse(warehouse);
        }
        if (includeSafetyStock != null) {
            request.setIncludeSafetyStock(includeSafetyStock);
        }
        if (includeOpenPurchaseOrders != null) {
            request.setIncludeOpenPurchaseOrders(includeOpenPurchaseOrders);
        }
        if (includeOpenWorkOrders != null) {
            request.setIncludeOpenWorkOrders(includeOpenWorkOrders);
        }
        return planningService.normalizeMrpRequest(request);
    }

    private void populateItemFormModel(Model model, ItemCreateRequest request) {
        model.addAttribute("pageTitle", "Yeni Kalem Oluştur");
        model.addAttribute("activePage", "inventory");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Planlama", "Yeni Kalem"));
        model.addAttribute("itemCreateRequest", request);
        model.addAttribute("uomOptions", planningService.getItemUnits());
    }
}
