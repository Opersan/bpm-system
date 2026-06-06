package com.erp.modules.purchaserequest.service;

import com.erp.modules.procurement.dto.CreatePORequest;
import com.erp.modules.procurement.dto.PurchaseOrderItemDto;
import com.erp.modules.procurement.entity.Item;
import com.erp.modules.procurement.entity.Supplier;
import com.erp.modules.procurement.repository.ItemRepository;
import com.erp.modules.procurement.repository.SupplierRepository;
import com.erp.modules.procurement.service.ProcurementService;
import com.erp.modules.purchaserequest.dto.ApprovalDto;
import com.erp.modules.purchaserequest.dto.PurchaseRequestCreateDto;
import com.erp.modules.purchaserequest.dto.QuoteEntryDto;
import com.erp.modules.purchaserequest.dto.TaskItemDto;
import com.erp.modules.purchaserequest.entity.PurchaseRequest;
import com.erp.modules.purchaserequest.entity.PurchaseRequestItem;
import com.erp.modules.purchaserequest.entity.PurchaseRequestQuote;
import com.erp.modules.purchaserequest.entity.PurchaseRequestStatus;
import com.erp.modules.purchaserequest.repository.PurchaseRequestQuoteRepository;
import com.erp.modules.purchaserequest.repository.PurchaseRequestRepository;
import com.erp.modules.usermanagement.dto.UserDto;
import com.erp.modules.usermanagement.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestQuoteRepository quoteRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final ProcurementService procurementService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicInteger dailySeq = new AtomicInteger(0);

    @Transactional
    public PurchaseRequest create(PurchaseRequestCreateDto dto, String requestedByUsername) {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setRequestNumber(generateRequestNumber());
        pr.setDescription(dto.getDescription());
        pr.setDepartment(dto.getDepartment());
        pr.setRequestedBy(requestedByUsername);
        pr.setRequiredBy(dto.getRequiredBy());
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);

        if (dto.getItems() != null) {
            for (PurchaseRequestCreateDto.PurchaseRequestItemDto itemDto : dto.getItems()) {
                if (itemDto.getItemName() == null || itemDto.getItemName().isBlank()) continue;
                PurchaseRequestItem pri = new PurchaseRequestItem();
                pri.setPurchaseRequest(pr);
                pri.setItemName(itemDto.getItemName());
                pri.setItemCode(itemDto.getItemCode());
                pri.setQuantity(itemDto.getQuantity());
                pri.setUom(itemDto.getUom());
                pri.setNotes(itemDto.getNotes());
                if (itemDto.getItemId() != null) {
                    itemRepository.findById(itemDto.getItemId()).ifPresent(item -> {
                        pri.setItem(item);
                        if (pri.getItemName() == null || pri.getItemName().isBlank()) {
                            pri.setItemName(item.getName());
                        }
                        if (pri.getItemCode() == null || pri.getItemCode().isBlank()) {
                            pri.setItemCode(item.getCode());
                        }
                    });
                }
                pr.getItems().add(pri);
            }
        }

        return purchaseRequestRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest enterQuotes(Long id, QuoteEntryDto dto, String purchaserUsername) {
        PurchaseRequest pr = getById(id);
        if (pr.getStatus() != PurchaseRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Bu talep teklif girişi için uygun durumda değil.");
        }
        pr.setHandledBy(purchaserUsername);
        pr.getQuotes().clear();

        for (QuoteEntryDto.QuoteLineDto line : dto.getQuotes()) {
            if (line.getSupplierId() == null || line.getTotalAmount() == null) continue;
            Supplier supplier = supplierRepository.findById(line.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Tedarikçi bulunamadı: " + line.getSupplierId()));

            PurchaseRequestQuote quote = new PurchaseRequestQuote();
            quote.setPurchaseRequest(pr);
            quote.setSupplier(supplier);
            quote.setSupplierName(supplier.getName());
            quote.setTotalAmount(line.getTotalAmount());
            quote.setCurrency(line.getCurrency() != null ? line.getCurrency() : "TRY");
            quote.setEstimatedDeliveryDate(line.getEstimatedDeliveryDate());
            quote.setNotes(line.getNotes());
            pr.getQuotes().add(quote);
        }

        if (pr.getQuotes().isEmpty()) {
            throw new IllegalStateException("En az bir teklif girilmelidir.");
        }

        pr.setStatus(PurchaseRequestStatus.QUOTES_ENTERED);
        return purchaseRequestRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest approve(Long id, ApprovalDto dto, String approverUsername) {
        PurchaseRequest pr = getById(id);
        if (pr.getStatus() != PurchaseRequestStatus.QUOTES_ENTERED) {
            throw new IllegalStateException("Bu talep onay için uygun durumda değil.");
        }

        PurchaseRequestQuote selectedQuote = pr.getQuotes().stream()
                .filter(q -> q.getId().equals(dto.getSelectedQuoteId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Seçilen teklif bulunamadı."));

        selectedQuote.setSelected(true);

        // Create Purchase Order from approved quote
        CreatePORequest poRequest = new CreatePORequest();
        poRequest.setSupplierId(selectedQuote.getSupplier().getId());
        poRequest.setOrderDate(LocalDate.now());
        poRequest.setExpectedDeliveryDate(selectedQuote.getEstimatedDeliveryDate());
        poRequest.setCurrency(selectedQuote.getCurrency());
        poRequest.setDescription("Satın Alma Talebi: " + pr.getRequestNumber() + " - " + pr.getDescription());
        poRequest.setCreatedBy(approverUsername);

        // Map PR items to PO items - use catalog items where linked
        List<PurchaseOrderItemDto> poItems = buildPoItems(pr, selectedQuote);
        poRequest.setItems(poItems);

        // When PR is approved by manager, the PO should be created in APPROVED status directly
        // so it can be used for receiving goods immediately
        var po = procurementService.createPOForApprovedRequest(poRequest);

        pr.setPurchaseOrderId(po.getId());
        pr.setApprovedBy(approverUsername);
        pr.setStatus(PurchaseRequestStatus.APPROVED);

        return purchaseRequestRepository.save(pr);
    }

    @Transactional
    public PurchaseRequest reject(Long id, String rejectionReason, String approverUsername) {
        PurchaseRequest pr = getById(id);
        if (pr.getStatus() == PurchaseRequestStatus.APPROVED || pr.getStatus() == PurchaseRequestStatus.REJECTED) {
            throw new IllegalStateException("Bu talep reddedilemez (mevcut durum: " + pr.getStatus().getLabel() + ").");
        }
        pr.setApprovedBy(approverUsername);
        pr.setRejectionReason(rejectionReason);
        pr.setStatus(PurchaseRequestStatus.REJECTED);
        return purchaseRequestRepository.save(pr);
    }

    public PurchaseRequest getById(Long id) {
        return purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Satın alma talebi bulunamadı: " + id));
    }

    public List<PurchaseRequest> getAll() {
        return purchaseRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<PurchaseRequest> getByRequestedBy(String username) {
        return purchaseRequestRepository.findByRequestedByOrderByCreatedAtDesc(username);
    }

    public List<PurchaseRequest> getByStatus(PurchaseRequestStatus status) {
        return purchaseRequestRepository.findByStatusOrderByCreatedAtAsc(status);
    }

    /**
     * Returns tasks relevant to the current user based on their role.
     */
    public List<TaskItemDto> getTasksForUser(UserDto currentUser) {
        List<PurchaseRequest> requests;
        String actionLabel;

        if (currentUser.getRole() == UserRole.MANAGER || currentUser.getRole() == UserRole.ADMIN) {
            requests = getByStatus(PurchaseRequestStatus.QUOTES_ENTERED);
            actionLabel = "İncele ve Onayla";
        } else if (currentUser.getRole() == UserRole.PURCHASING_USER) {
            requests = getByStatus(PurchaseRequestStatus.SUBMITTED);
            actionLabel = "Teklif Gir";
        } else if (currentUser.getRole() == UserRole.PRODUCTION_USER) {
            requests = getByRequestedBy(currentUser.getUsername()).stream()
                    .filter(r -> r.getStatus() == PurchaseRequestStatus.SUBMITTED
                              || r.getStatus() == PurchaseRequestStatus.QUOTES_ENTERED)
                    .toList();
            actionLabel = "Takip Et";
        } else {
            requests = new ArrayList<>();
            actionLabel = "Görüntüle";
        }

        final String label = actionLabel;
        return requests.stream().map(pr -> toTaskItem(pr, label)).toList();
    }

    public TaskItemDto toTaskItem(PurchaseRequest pr) {
        return toTaskItem(pr, null);
    }

    public TaskItemDto toTaskItem(PurchaseRequest pr, String actionLabel) {
        return TaskItemDto.builder()
                .id(pr.getId())
                .requestNumber(pr.getRequestNumber())
                .description(pr.getDescription())
                .requestedBy(pr.getRequestedBy())
                .department(pr.getDepartment())
                .requiredBy(pr.getRequiredBy())
                .status(pr.getStatus())
                .createdAt(pr.getCreatedAt())
                .itemCount(pr.getItems().size())
                .quoteCount(pr.getQuotes().size())
                .actionLabel(actionLabel)
                .build();
    }

    private String generateRequestNumber() {
        String dateStr = LocalDate.now().format(DATE_FMT);
        String base = "PT-" + dateStr + "-";
        int seq = 1;
        String candidate;
        do {
            candidate = base + String.format("%04d", seq++);
        } while (purchaseRequestRepository.existsByRequestNumber(candidate));
        return candidate;
    }

    private List<PurchaseOrderItemDto> buildPoItems(PurchaseRequest pr, PurchaseRequestQuote selectedQuote) {
        List<PurchaseOrderItemDto> poItems = new ArrayList<>();

        // Distribute total amount equally across items for a simple PO line
        int itemCount = pr.getItems().isEmpty() ? 1 : pr.getItems().size();

        for (PurchaseRequestItem pri : pr.getItems()) {
            PurchaseOrderItemDto poItem = new PurchaseOrderItemDto();
            if (pri.getItem() != null) {
                poItem.setItemId(pri.getItem().getId());
                // Use catalog price or estimate from quote
                java.math.BigDecimal unitPrice = selectedQuote.getTotalAmount()
                        .divide(java.math.BigDecimal.valueOf(itemCount), 2, java.math.RoundingMode.HALF_UP);
                poItem.setPrice(unitPrice);
            } else {
                // Try to find by code
                if (pri.getItemCode() != null && !pri.getItemCode().isBlank()) {
                    Item catalogItem = itemRepository.findByCode(pri.getItemCode()).orElse(null);
                    if (catalogItem != null) {
                        poItem.setItemId(catalogItem.getId());
                        java.math.BigDecimal unitPrice = selectedQuote.getTotalAmount()
                                .divide(java.math.BigDecimal.valueOf(itemCount), 2, java.math.RoundingMode.HALF_UP);
                        poItem.setPrice(unitPrice);
                    }
                }
            }
            if (poItem.getItemId() == null) {
                // Skip items without catalog match - PO description covers the request
                continue;
            }
            poItem.setQuantity(pri.getQuantity() != null ? pri.getQuantity().intValue() : 1);
            poItem.setVatRate(java.math.BigDecimal.valueOf(20));
            poItems.add(poItem);
        }

        // Fallback: if no items matched catalog, create a summary line using first available item
        if (poItems.isEmpty()) {
            List<Item> allItems = itemRepository.findAll();
            if (!allItems.isEmpty()) {
                PurchaseOrderItemDto fallback = new PurchaseOrderItemDto();
                fallback.setItemId(allItems.get(0).getId());
                fallback.setQuantity(1);
                fallback.setPrice(selectedQuote.getTotalAmount());
                fallback.setVatRate(java.math.BigDecimal.valueOf(20));
                poItems.add(fallback);
            }
        }

        return poItems;
    }
}
