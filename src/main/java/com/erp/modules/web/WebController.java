package com.erp.modules.web;

import com.erp.modules.inventory.service.InventoryService;
import com.erp.modules.manufacturing.service.ManufacturingService;
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
    private final ManufacturingService manufacturingService;

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
        model.addAttribute("pageTitle", "Kontrol Paneli");
        model.addAttribute("activePage", "dashboard");
        
        // KPI Data
        model.addAttribute("totalOrders", procurementService.getAllOrders().size());
        model.addAttribute("totalStock", inventoryService.getAllStock().size());
        model.addAttribute("totalWorkOrders", manufacturingService.getAllWorkOrders().size());
        model.addAttribute("pendingOrders", procurementService.getAllOrders().stream()
            .filter(o -> o.getStatus().toString().equals("DRAFT") || o.getStatus().toString().equals("PENDING_APPROVAL"))
            .count());
        
        // Recent orders for table
        var recentOrders = procurementService.getAllOrders().stream()
            .sorted((a, b) -> b.getId().compareTo(a.getId()))
            .limit(5)
            .toList();
        model.addAttribute("recentOrders", recentOrders);
        
        // Low stock items
        var lowStockItems = inventoryService.getAllStock().stream()
            .filter(s -> s.getOnHand() < 10)
            .limit(5)
            .toList();
        model.addAttribute("lowStockItems", lowStockItems);
        
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
