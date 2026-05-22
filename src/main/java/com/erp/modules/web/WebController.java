package com.erp.modules.web;

import com.erp.modules.purchaserequest.service.PurchaseRequestService;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.service.AuthorizationService;
import com.erp.modules.inventory.service.InventoryService;
import com.erp.modules.procurement.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final ProcurementService procurementService;
    private final InventoryService inventoryService;
    private final PurchaseRequestService purchaseRequestService;
    private final AuthorizationService authorizationService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("activePage", "dashboard");

        UserDto currentUser = authorizationService.getCurrentUser();
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("tasks", purchaseRequestService.getTasksForUser(currentUser));

        return "dashboard";
    }

    @GetMapping("/procurement")
    public String procurement(Model model) {
        return "redirect:/purchasing/orders";
    }

    @GetMapping("/procurement/new")
    public String newOrder(Model model) {
        return "redirect:/purchasing/new";
    }

    @GetMapping("/inventory")
    public String inventory(Model model) {
        return "redirect:/planning/inventory";
    }

    @GetMapping("/inventory/receive")
    public String receiveGoods(Model model) {
        model.addAttribute("pageTitle", "Mal Kabul");
        model.addAttribute("activePage", "inventory");
        model.addAttribute("pendingOrders", inventoryService.getPendingPurchaseOrders());
        model.addAttribute("warehouses", inventoryService.getAllWarehouses());
        model.addAttribute("items", procurementService.getAllItems());
        return "inventory/receive";
    }

    @GetMapping("/manufacturing")
    public String manufacturing(Model model) {
        return "redirect:/production/work-orders";
    }

    @GetMapping("/manufacturing/new")
    public String newWorkOrder(Model model) {
        return "redirect:/production/work-orders/new";
    }

    @GetMapping("/mrp")
    public String mrp(Model model) {
        return "redirect:/planning/mrp";
    }

    @GetMapping("/mrp/run")
    public String runMrp(Model model) {
        return "redirect:/planning/mrp";
    }
}
