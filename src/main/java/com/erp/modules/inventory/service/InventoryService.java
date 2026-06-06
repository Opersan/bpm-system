package com.erp.modules.inventory.service;

import com.erp.modules.inventory.dto.GoodsReceiptItemDto;
import com.erp.modules.inventory.dto.GoodsReceiptRequest;
import com.erp.modules.inventory.entity.*;
import com.erp.modules.inventory.repository.InventoryTransactionRepository;
import com.erp.modules.inventory.repository.StockRepository;
import com.erp.modules.inventory.repository.WarehouseRepository;
import com.erp.modules.procurement.entity.Item;
import com.erp.modules.procurement.entity.POStatus;
import com.erp.modules.procurement.entity.PurchaseOrder;
import com.erp.modules.procurement.repository.ItemRepository;
import com.erp.modules.procurement.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.modules.inventory.dto.StockMovementDto;
import com.erp.modules.inventory.dto.StockOperationRequest;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.service.UserService;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockRepository stockRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    @Transactional
    public void processGoodsReceipt(GoodsReceiptRequest request) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new RuntimeException("PO not found"));

        if (po.getStatus() != POStatus.APPROVED && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new RuntimeException("PO must be APPROVED or PARTIALLY_RECEIVED to receive goods");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        for (GoodsReceiptItemDto itemDto : request.getItems()) {
            Item item = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            // 1. Create Transaction
            InventoryTransaction transaction = new InventoryTransaction();
            transaction.setItem(item);
            transaction.setWarehouse(warehouse);
            transaction.setType(TransactionType.IN);
            transaction.setQuantity(itemDto.getQuantity());
            transaction.setReferenceId(po.getId());
            transaction.setReferenceType("PO");
            transactionRepository.save(transaction);

            // 2. Update Stock
            Stock stock = stockRepository.findByItemAndWarehouse(item, warehouse)
                    .orElse(new Stock(item, warehouse, 0, null));
            
            stock.setOnHand(stock.getOnHand() + itemDto.getQuantity());
            stockRepository.save(stock);
        }

        // Update PO Status (Simplified logic: assume fully received if any receipt happens for now, or keep PARTIALLY_RECEIVED)
        // In a real system, we would check if all items are fully received.
        po.setStatus(POStatus.PARTIALLY_RECEIVED); // Or CLOSED
        purchaseOrderRepository.save(po);
    }

    public java.util.List<Stock> getAllStock() {
        return stockRepository.findAll();
    }
    public List<PurchaseOrder> getPendingPurchaseOrders() {
        return purchaseOrderRepository.findByStatusIn(java.util.List.of(POStatus.APPROVED, POStatus.PARTIALLY_RECEIVED));
    }

    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Transactional
    public void performStockIn(StockOperationRequest request) {
        System.out.println("performStockIn called with: itemId=" + request.getItemId() + ", warehouseId=" + request.getWarehouseId());
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> {
                    System.out.println("Item not found for ID: " + request.getItemId());
                    return new RuntimeException("Item not found");
                });

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Miktar pozitif olmalıdır.");
        }

        if (request.getNote() == null || request.getNote().trim().isEmpty()) {
            throw new IllegalArgumentException("Açıklama zorunludur.");
        }

        Stock stock = stockRepository.findByItemAndWarehouse(item, warehouse)
                .orElse(new Stock(item, warehouse, 0, null));

        int previousQuantity = stock.getOnHand();
        int newQuantity = previousQuantity + request.getQuantity();

        // Create Transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setItem(item);
        transaction.setWarehouse(warehouse);
        transaction.setType(TransactionType.IN);
        transaction.setQuantity(request.getQuantity());
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setNote(request.getNote());
        transaction.setReferenceId(null);
        transaction.setReferenceType("MANUAL");
        transactionRepository.save(transaction);

        // Update Stock
        stock.setOnHand(newQuantity);
        stockRepository.save(stock);
    }

    @Transactional
    public void performStockOut(StockOperationRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Miktar pozitif olmalıdır.");
        }

        if (request.getNote() == null || request.getNote().trim().isEmpty()) {
            throw new IllegalArgumentException("Açıklama zorunludur.");
        }

        Stock stock = stockRepository.findByItemAndWarehouse(item, warehouse)
                .orElse(new Stock(item, warehouse, 0, null));

        if (stock.getOnHand() < request.getQuantity()) {
            throw new IllegalArgumentException("Yetersiz stok. Mevcut stok: " + stock.getOnHand());
        }

        int previousQuantity = stock.getOnHand();
        int newQuantity = previousQuantity - request.getQuantity();

        // Create Transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setItem(item);
        transaction.setWarehouse(warehouse);
        transaction.setType(TransactionType.OUT);
        transaction.setQuantity(request.getQuantity());
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setNote(request.getNote());
        transaction.setReferenceId(null);
        transaction.setReferenceType("MANUAL");
        transactionRepository.save(transaction);

        // Update Stock
        stock.setOnHand(newQuantity);
        stockRepository.save(stock);
    }

    @Transactional
    public void performStockAdjustment(StockOperationRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Item not found"));

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new RuntimeException("Warehouse not found"));

        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new IllegalArgumentException("Yeni stok miktarı negatif olamaz.");
        }

        if (request.getNote() == null || request.getNote().trim().isEmpty()) {
            throw new IllegalArgumentException("Açıklama zorunludur.");
        }

        Stock stock = stockRepository.findByItemAndWarehouse(item, warehouse)
                .orElse(new Stock(item, warehouse, 0, null));

        int previousQuantity = stock.getOnHand();
        int newQuantity = request.getQuantity();

        // Determine transaction type based on adjustment direction
        TransactionType type = newQuantity > previousQuantity ? TransactionType.IN :
                               (newQuantity < previousQuantity ? TransactionType.OUT : TransactionType.ADJUSTMENT);

        // Create Transaction
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setItem(item);
        transaction.setWarehouse(warehouse);
        transaction.setType(type);
        transaction.setQuantity(Math.abs(newQuantity - previousQuantity));
        transaction.setPreviousQuantity(previousQuantity);
        transaction.setNewQuantity(newQuantity);
        transaction.setNote(request.getNote());
        transaction.setReferenceId(null);
        transaction.setReferenceType("MANUAL_ADJUSTMENT");
        transactionRepository.save(transaction);

        // Update Stock
        stock.setOnHand(newQuantity);
        stockRepository.save(stock);
    }

    public List<StockMovementDto> getStockMovements(Long itemId, Long warehouseId) {
        List<InventoryTransaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getItem().getId().equals(itemId) &&
                           t.getWarehouse().getId().equals(warehouseId))
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .toList();

        UserDto currentUser = userService.getCurrentUser();

        return transactions.stream()
                .map(this::toStockMovementDto)
                .toList();
    }

    private StockMovementDto toStockMovementDto(InventoryTransaction transaction) {
        return new StockMovementDto(
                transaction.getId(),
                transaction.getCreatedAt(),
                transaction.getType(),
                transaction.getQuantity(),
                transaction.getPreviousQuantity(),
                transaction.getNewQuantity(),
                transaction.getWarehouse().getName(),
                transaction.getReferenceId(),
                transaction.getReferenceType(),
                transaction.getNote(),
                "Sistem" // In real implementation, get user from transaction or current user
        );
    }
}
