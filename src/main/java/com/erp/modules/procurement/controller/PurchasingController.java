package com.erp.modules.procurement.controller;

import com.erp.modules.procurement.dto.CreatePORequest;
import com.erp.modules.procurement.dto.ProcurementSummaryCardDto;
import com.erp.modules.procurement.dto.PurchaseOrderFilterDto;
import com.erp.modules.procurement.dto.PurchaseOrderItemDto;
import com.erp.modules.procurement.dto.SupplierCreateRequest;
import com.erp.modules.procurement.entity.ApprovalStatus;
import com.erp.modules.procurement.entity.POStatus;
import com.erp.modules.procurement.entity.PurchaseOrder;
import com.erp.modules.procurement.entity.Supplier;
import com.erp.modules.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/purchasing")
@RequiredArgsConstructor
public class PurchasingController {

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    private final ProcurementService procurementService;

    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String approvalStatus,
                         @RequestParam(required = false) Long supplierId,
                         Model model) {
        PurchaseOrderFilterDto filters = createFilter(search, status, approvalStatus, supplierId);
        List<PurchaseOrder> orders = procurementService.getFilteredOrders(filters);

        model.addAttribute("pageTitle", "Satın Alma Siparişleri");
        model.addAttribute("activePage", "procurement");
        model.addAttribute("orders", orders);
        model.addAttribute("suppliers", procurementService.getAllSuppliers());
        model.addAttribute("statuses", POStatus.values());
        model.addAttribute("approvalStatuses", ApprovalStatus.values());
        model.addAttribute("filters", filters);
        model.addAttribute("summaryCards", createSummaryCards(orders));
        return "purchasing/orders";
    }

    @GetMapping("/new")
    public String newOrder(@RequestParam(required = false) String materialCode,
                           @RequestParam(required = false) Long supplierId,
                           Model model) {
        model.addAttribute("pageTitle", "Yeni Satın Alma Siparişi");
        model.addAttribute("activePage", "procurement");
        model.addAttribute("suppliers", procurementService.getAllSuppliers());
        model.addAttribute("items", procurementService.getAllItems());
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("materialCode", materialCode);
        model.addAttribute("selectedSupplierId", supplierId);
        return "purchasing/new";
    }

    @GetMapping("/suppliers/new")
    public String newSupplier(Model model) {
        populateSupplierFormModel(model, new SupplierCreateRequest());
        return "purchasing/supplier-new";
    }

    @PostMapping("/suppliers")
    public String createSupplier(@Valid @ModelAttribute("supplierCreateRequest") SupplierCreateRequest supplierCreateRequest,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", "Lütfen zorunlu alanları kontrol edin.");
            populateSupplierFormModel(model, supplierCreateRequest);
            return "purchasing/supplier-new";
        }

        Supplier supplier;
        try {
            supplier = procurementService.createSupplier(supplierCreateRequest);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            populateSupplierFormModel(model, supplierCreateRequest);
            return "purchasing/supplier-new";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Yeni tedarikçi oluşturuldu.");
        return "redirect:/purchasing/suppliers";
    }

    @PostMapping("/orders")
    public String createOrder(@RequestParam Long supplierId,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDeliveryDate,
                              @RequestParam(defaultValue = "TRY") String currency,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) List<Long> itemId,
                              @RequestParam(required = false) List<Integer> quantity,
                              @RequestParam(required = false) List<BigDecimal> price,
                              @RequestParam(required = false) List<BigDecimal> vatRate,
                              @RequestParam(defaultValue = "draft") String submitAction,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        CreatePORequest request = new CreatePORequest();
        request.setSupplierId(supplierId);
        request.setOrderDate(orderDate);
        request.setExpectedDeliveryDate(expectedDeliveryDate);
        request.setCurrency(currency);
        request.setDescription(description);
        request.setCreatedBy(principal != null ? principal.getName() : "system");
        request.setItems(createItems(itemId, quantity, price, vatRate));

        boolean submitForApproval = "pending_approval".equals(submitAction);
        PurchaseOrder order = procurementService.createPO(request, submitForApproval);
        redirectAttributes.addFlashAttribute("successMessage",
            submitForApproval ? "Satın alma siparişi onaya gönderildi." : "Satın alma siparişi taslak olarak kaydedildi.");
        return "redirect:/purchasing/orders?search=SAS-" + order.getId();
    }

    @PostMapping("/orders/submit")
    public String submitOrder(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        procurementService.submitForApproval(id);
        redirectAttributes.addFlashAttribute("successMessage", "Sipariş onaya gönderildi.");
        return "redirect:/purchasing/orders";
    }

    @PostMapping("/orders/cancel")
    public String cancelOrder(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        procurementService.cancelPO(id);
        redirectAttributes.addFlashAttribute("successMessage", "Sipariş iptal edildi.");
        return "redirect:/purchasing/orders";
    }

    private PurchaseOrderFilterDto createFilter(String search, String status, String approvalStatus, Long supplierId) {
        PurchaseOrderFilterDto filter = new PurchaseOrderFilterDto();
        filter.setSearch(search);
        filter.setStatus(parseStatus(status));
        filter.setApprovalStatus(parseApprovalStatus(approvalStatus));
        filter.setSupplierId(supplierId);
        return filter;
    }

    private List<PurchaseOrderItemDto> createItems(List<Long> itemIds, List<Integer> quantities,
                                                   List<BigDecimal> prices, List<BigDecimal> vatRates) {
        List<PurchaseOrderItemDto> items = new ArrayList<>();
        if (itemIds == null) {
            return items;
        }

        for (int index = 0; index < itemIds.size(); index++) {
            Long selectedItemId = itemIds.get(index);
            if (selectedItemId == null) {
                continue;
            }

            PurchaseOrderItemDto item = new PurchaseOrderItemDto();
            item.setItemId(selectedItemId);
            item.setQuantity(valueAt(quantities, index, 1));
            item.setPrice(valueAt(prices, index, BigDecimal.ZERO));
            item.setVatRate(valueAt(vatRates, index, BigDecimal.valueOf(20)));
            items.add(item);
        }
        return items;
    }

    private List<ProcurementSummaryCardDto> createSummaryCards(List<PurchaseOrder> orders) {
        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthlyAmount = orders.stream()
            .filter(order -> order.getOrderDate() != null && YearMonth.from(order.getOrderDate()).equals(currentMonth))
            .map(PurchaseOrder::getTotalAmount)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return List.of(
            ProcurementSummaryCardDto.builder().label("Toplam Sipariş").value(String.valueOf(orders.size())).icon("fas fa-file-invoice").variant("primary").build(),
            ProcurementSummaryCardDto.builder().label("Onay Bekleyen").value(String.valueOf(countStatus(orders, POStatus.PENDING_APPROVAL))).icon("fas fa-hourglass-half").variant("warning").build(),
            ProcurementSummaryCardDto.builder().label("Açık Sipariş").value(String.valueOf(orders.stream().filter(order -> order.getStatus() == POStatus.PENDING_APPROVAL || order.getStatus() == POStatus.APPROVED || order.getStatus() == POStatus.SENT).count())).icon("fas fa-clipboard-check").variant("info").build(),
            ProcurementSummaryCardDto.builder().label("Kısmi Teslim").value(String.valueOf(countStatus(orders, POStatus.PARTIALLY_RECEIVED))).icon("fas fa-truck-loading").variant("success").build(),
            ProcurementSummaryCardDto.builder().label("Bu Ayki Toplam Tutar").value(formatCurrency(monthlyAmount)).icon("fas fa-coins").variant("success").build()
        );
    }

    private long countStatus(List<PurchaseOrder> orders, POStatus status) {
        return orders.stream().filter(order -> order.getStatus() == status).count();
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(TR_LOCALE).format(value);
    }

    private POStatus parseStatus(String value) {
        try {
            return value == null || value.isBlank() ? null : POStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private ApprovalStatus parseApprovalStatus(String value) {
        try {
            return value == null || value.isBlank() ? null : ApprovalStatus.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private <T> T valueAt(List<T> values, int index, T fallback) {
        if (values == null || index >= values.size() || values.get(index) == null) {
            return fallback;
        }
        return values.get(index);
    }

    private void populateSupplierFormModel(Model model, SupplierCreateRequest request) {
        model.addAttribute("pageTitle", "Yeni Tedarikçi");
        model.addAttribute("activePage", "procurement");
        model.addAttribute("breadcrumbs", List.of("Dashboard", "Satın Alma", "Yeni Tedarikçi"));
        model.addAttribute("supplierCreateRequest", request);
    }

    @GetMapping("/suppliers")
    public String supplierList(Model model,
                               @ModelAttribute("successMessage") String successMessage) {
        model.addAttribute("suppliers", procurementService.getAllSuppliers());
        model.addAttribute("pageTitle", "Tedarikçiler");
        model.addAttribute("activePage", "procurement");
        return "purchasing/suppliers";
    }

    @GetMapping("/suppliers/{id}/edit")
    public String supplierEditForm(@PathVariable Long id, Model model) {
        var supplier = procurementService.getSupplierById(id);
        var request = new SupplierCreateRequest();
        request.setName(supplier.getName());
        request.setContactEmail(supplier.getContactEmail());
        request.setAddress(supplier.getAddress());
        model.addAttribute("supplierId", id);
        model.addAttribute("supplierCreateRequest", request);
        model.addAttribute("pageTitle", "Tedarikçi Düzenle");
        model.addAttribute("activePage", "procurement");
        return "purchasing/supplier-edit";
    }

    @PostMapping("/suppliers/{id}")
    public String supplierUpdate(@PathVariable Long id,
                                 @Valid @ModelAttribute SupplierCreateRequest supplierCreateRequest,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("supplierId", id);
            model.addAttribute("errorMessage", "Lütfen formdaki hataları düzeltin.");
            return "purchasing/supplier-edit";
        }
        try {
            procurementService.updateSupplier(id, supplierCreateRequest);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("name", "duplicate", ex.getMessage());
            model.addAttribute("supplierId", id);
            model.addAttribute("errorMessage", ex.getMessage());
            return "purchasing/supplier-edit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Tedarikçi güncellendi.");
        return "redirect:/purchasing/suppliers";
    }
}
