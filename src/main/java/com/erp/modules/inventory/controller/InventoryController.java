package com.erp.modules.inventory.controller;

import com.erp.modules.inventory.dto.GoodsReceiptRequest;
import com.erp.modules.inventory.dto.StockMovementDto;
import com.erp.modules.inventory.dto.StockOperationRequest;
import com.erp.modules.inventory.service.InventoryService;
import com.erp.modules.usermanagement.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.erp.modules.inventory.entity.Stock;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final AuthorizationService authorizationService;

    @PostMapping("/receipts")
    public ResponseEntity<String> receiveGoods(@RequestBody GoodsReceiptRequest request) {
        inventoryService.processGoodsReceipt(request);
        return ResponseEntity.ok("Goods received successfully");
    }

    @GetMapping("/stock")
    public ResponseEntity<List<Stock>> getAllStock() {
        return ResponseEntity.ok(inventoryService.getAllStock());
    }

    @PostMapping("/stock-in")
    public ResponseEntity<Map<String, Object>> stockIn(@RequestBody StockOperationRequest request) {
        try {
            System.out.println("Stock In Request: itemId=" + request.getItemId() + ", warehouseId=" + request.getWarehouseId() + ", quantity=" + request.getQuantity() + ", note=" + request.getNote());
            inventoryService.performStockIn(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stok girişi başarıyla yapıldı.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Hata: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/stock-out")
    public ResponseEntity<Map<String, Object>> stockOut(@RequestBody StockOperationRequest request) {
        try {
            System.out.println("Stock Out Request: itemId=" + request.getItemId() + ", warehouseId=" + request.getWarehouseId() + ", quantity=" + request.getQuantity() + ", note=" + request.getNote());
            inventoryService.performStockOut(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stok çıkışı başarıyla yapıldı.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Hata: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/stock-adjust")
    public ResponseEntity<Map<String, Object>> stockAdjust(@RequestBody StockOperationRequest request) {
        try {
            System.out.println("Stock Adjust Request: itemId=" + request.getItemId() + ", warehouseId=" + request.getWarehouseId() + ", quantity=" + request.getQuantity() + ", note=" + request.getNote());
            inventoryService.performStockAdjustment(request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Stok düzeltme başarıyla yapıldı.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Hata: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<List<StockMovementDto>> getStockMovements(
            @RequestParam Long itemId,
            @RequestParam Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getStockMovements(itemId, warehouseId));
    }
}
