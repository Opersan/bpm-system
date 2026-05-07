package com.erp.modules.procurement.service;

import com.erp.modules.procurement.dto.CreatePORequest;
import com.erp.modules.procurement.dto.PurchaseOrderFilterDto;
import com.erp.modules.procurement.dto.PurchaseOrderItemDto;
import com.erp.modules.procurement.dto.SupplierCreateRequest;
import com.erp.modules.procurement.entity.*;
import com.erp.modules.procurement.repository.ItemRepository;
import com.erp.modules.procurement.repository.PurchaseOrderRepository;
import com.erp.modules.procurement.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    @Transactional
    public PurchaseOrder createPO(CreatePORequest request) {
        return createPO(request, false);
    }

    @Transactional
    public PurchaseOrder createPO(CreatePORequest request, boolean submitForApproval) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        PurchaseOrder po = new PurchaseOrder();
        po.setSupplier(supplier);
        po.setStatus(submitForApproval ? POStatus.PENDING_APPROVAL : POStatus.DRAFT);
        po.setApprovalStatus(submitForApproval ? ApprovalStatus.WAITING : ApprovalStatus.NOT_SUBMITTED);
        po.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        po.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        po.setCurrency(request.getCurrency() != null && !request.getCurrency().isBlank() ? request.getCurrency() : "TRY");
        po.setDescription(request.getDescription());
        po.setCreatedBy(request.getCreatedBy());
        
        List<PurchaseOrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("At least one item is required");
        }

        for (PurchaseOrderItemDto itemDto : request.getItems()) {
            if (itemDto.getItemId() == null) {
                continue;
            }
            if (itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new RuntimeException("Quantity must be greater than zero");
            }
            if (itemDto.getPrice() == null || itemDto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Price cannot be negative");
            }

            Item item = itemRepository.findById(itemDto.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemDto.getItemId()));

            PurchaseOrderItem poItem = new PurchaseOrderItem();
            poItem.setPurchaseOrder(po);
            poItem.setItem(item);
            poItem.setQuantity(itemDto.getQuantity());
            poItem.setPrice(itemDto.getPrice());
            poItem.setVatRate(itemDto.getVatRate() != null ? itemDto.getVatRate() : BigDecimal.valueOf(20));
            
            items.add(poItem);
            totalAmount = totalAmount.add(poItem.getLineTotal().add(poItem.getVatAmount()));
        }

        if (items.isEmpty()) {
            throw new RuntimeException("At least one valid item is required");
        }

        po.setItems(items);
        po.setTotalAmount(totalAmount);

        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public Supplier createSupplier(SupplierCreateRequest request) {
        String supplierName = normalizeText(request.getName());
        if (supplierRepository.existsByNameIgnoreCase(supplierName)) {
            throw new IllegalArgumentException("Bu tedarikçi zaten kayıtlı.");
        }

        Supplier supplier = new Supplier();
        supplier.setName(supplierName);
        supplier.setContactEmail(normalizeText(request.getContactEmail()));
        supplier.setAddress(normalizeText(request.getAddress()));
        return supplierRepository.save(supplier);
    }

    @Transactional
    public PurchaseOrder approvePO(Long poId) {
        PurchaseOrder po = purchaseOrderRepository.findById(poId)
                .orElseThrow(() -> new RuntimeException("PO not found"));

        if (po.getStatus() != POStatus.DRAFT && po.getStatus() != POStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Only DRAFT or PENDING_APPROVAL POs can be approved");
        }

        po.setStatus(POStatus.APPROVED);
        po.setApprovalStatus(ApprovalStatus.APPROVED);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder submitForApproval(Long poId) {
        PurchaseOrder po = getPO(poId);
        if (po.getStatus() != POStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT POs can be submitted for approval");
        }
        po.setStatus(POStatus.PENDING_APPROVAL);
        po.setApprovalStatus(ApprovalStatus.WAITING);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder cancelPO(Long poId) {
        PurchaseOrder po = getPO(poId);
        po.setStatus(POStatus.CANCELLED);
        return purchaseOrderRepository.save(po);
    }
    
    public PurchaseOrder getPO(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PO not found"));
    }

    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(PurchaseOrder::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<PurchaseOrder> getFilteredOrders(PurchaseOrderFilterDto filter) {
        return getAllOrders().stream()
                .filter(order -> matchesOrderSearch(order, filter.getSearch()))
                .filter(order -> filter.getStatus() == null || order.getStatus() == filter.getStatus())
                .filter(order -> filter.getApprovalStatus() == null || order.getApprovalStatus() == filter.getApprovalStatus())
                .filter(order -> filter.getSupplierId() == null || order.getSupplier().getId().equals(filter.getSupplierId()))
                .toList();
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    private boolean matchesOrderSearch(PurchaseOrder order, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }

        String normalized = search.toLowerCase(TR_LOCALE);
        String orderNo = "SAS-" + order.getId();
        return orderNo.toLowerCase(TR_LOCALE).contains(normalized)
                || order.getSupplier().getName().toLowerCase(TR_LOCALE).contains(normalized);
    }

    private String normalizeText(String value) {
        return value != null ? value.trim() : null;
    }
}
