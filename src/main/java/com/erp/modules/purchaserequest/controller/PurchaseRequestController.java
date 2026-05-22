package com.erp.modules.purchaserequest.controller;

import com.erp.modules.procurement.repository.ItemRepository;
import com.erp.modules.procurement.repository.SupplierRepository;
import com.erp.modules.purchaserequest.dto.ApprovalDto;
import com.erp.modules.purchaserequest.dto.PurchaseRequestCreateDto;
import com.erp.modules.purchaserequest.dto.QuoteEntryDto;
import com.erp.modules.purchaserequest.entity.PurchaseRequest;
import com.erp.modules.purchaserequest.entity.PurchaseRequestStatus;
import com.erp.modules.purchaserequest.service.PurchaseRequestService;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.model.UserRole;
import com.erp.modules.usermanagement.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final AuthorizationService authorizationService;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;

    @GetMapping
    public String list(Model model) {
        UserDto currentUser = authorizationService.getCurrentUser();
        model.addAttribute("pageTitle", "Satın Alma Talepleri");
        model.addAttribute("activePage", "purchase-requests");

        if (currentUser.getRole() == UserRole.PRODUCTION_USER) {
            model.addAttribute("requests", purchaseRequestService.getByRequestedBy(currentUser.getUsername()));
        } else {
            model.addAttribute("requests", purchaseRequestService.getAll());
        }
        return "purchase-request/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("pageTitle", "Yeni Satın Alma Talebi");
        model.addAttribute("activePage", "purchase-requests");
        model.addAttribute("form", new PurchaseRequestCreateDto());
        model.addAttribute("catalogItems", itemRepository.findAll());
        return "purchase-request/new";
    }

    @PostMapping
    public String create(@ModelAttribute PurchaseRequestCreateDto form, RedirectAttributes redirectAttrs) {
        UserDto currentUser = authorizationService.getCurrentUser();
        try {
            PurchaseRequest pr = purchaseRequestService.create(form, currentUser.getUsername());
            redirectAttrs.addFlashAttribute("successMessage",
                    "Satın alma talebi oluşturuldu: " + pr.getRequestNumber());
            return "redirect:/purchase-requests/" + pr.getId();
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/purchase-requests/new";
        }
    }

    @GetMapping("/{id}")
    public String process(@PathVariable Long id, Model model) {
        UserDto currentUser = authorizationService.getCurrentUser();
        PurchaseRequest pr = purchaseRequestService.getById(id);

        model.addAttribute("pageTitle", "Talep: " + pr.getRequestNumber());
        model.addAttribute("activePage", "purchase-requests");
        model.addAttribute("pr", pr);
        model.addAttribute("currentUser", currentUser);

        boolean canEnterQuotes = (currentUser.getRole() == UserRole.PURCHASING_USER || currentUser.getRole() == UserRole.ADMIN)
                && pr.getStatus() == PurchaseRequestStatus.SUBMITTED;
        boolean canApprove = (currentUser.getRole() == UserRole.MANAGER || currentUser.getRole() == UserRole.ADMIN)
                && pr.getStatus() == PurchaseRequestStatus.QUOTES_ENTERED;

        model.addAttribute("canEnterQuotes", canEnterQuotes);
        model.addAttribute("canApprove", canApprove);
        model.addAttribute("quoteForm", new QuoteEntryDto());
        model.addAttribute("approvalForm", new ApprovalDto());
        model.addAttribute("suppliers", supplierRepository.findAll());

        return "purchase-request/process";
    }

    @PostMapping("/{id}/quotes")
    public String submitQuotes(@PathVariable Long id,
                               @ModelAttribute QuoteEntryDto form,
                               RedirectAttributes redirectAttrs) {
        UserDto currentUser = authorizationService.getCurrentUser();
        try {
            purchaseRequestService.enterQuotes(id, form, currentUser.getUsername());
            redirectAttrs.addFlashAttribute("successMessage", "Teklifler kaydedildi. Yönetici onayına gönderildi.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-requests/" + id;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @ModelAttribute ApprovalDto form,
                          RedirectAttributes redirectAttrs) {
        UserDto currentUser = authorizationService.getCurrentUser();
        try {
            PurchaseRequest pr = purchaseRequestService.approve(id, form, currentUser.getUsername());
            redirectAttrs.addFlashAttribute("successMessage",
                    "Talep onaylandı ve satın alma siparişine dönüştürüldü. PO ID: " + pr.getPurchaseOrderId());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-requests/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String rejectionReason,
                         RedirectAttributes redirectAttrs) {
        UserDto currentUser = authorizationService.getCurrentUser();
        try {
            purchaseRequestService.reject(id, rejectionReason, currentUser.getUsername());
            redirectAttrs.addFlashAttribute("successMessage", "Talep reddedildi.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchase-requests/" + id;
    }
}
