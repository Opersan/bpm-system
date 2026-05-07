package com.erp.modules.planning.service;

import com.erp.modules.inventory.entity.Stock;
import com.erp.modules.inventory.entity.Warehouse;
import com.erp.modules.inventory.repository.StockRepository;
import com.erp.modules.inventory.repository.WarehouseRepository;
import com.erp.modules.manufacturing.entity.WOStatus;
import com.erp.modules.manufacturing.entity.WorkOrder;
import com.erp.modules.manufacturing.entity.WorkOrderRequirement;
import com.erp.modules.manufacturing.repository.WorkOrderRepository;
import com.erp.modules.manufacturing.repository.WorkOrderRequirementRepository;
import com.erp.modules.planning.dto.InventoryFilterDto;
import com.erp.modules.planning.dto.InventoryItemDto;
import com.erp.modules.planning.dto.InventoryStatus;
import com.erp.modules.planning.dto.ItemCreateRequest;
import com.erp.modules.planning.dto.MrpActionType;
import com.erp.modules.planning.dto.MrpRequestDto;
import com.erp.modules.planning.dto.MrpResultDto;
import com.erp.modules.planning.dto.MrpStatus;
import com.erp.modules.planning.dto.SummaryCardDto;
import com.erp.modules.planning.dto.WarehouseSummaryDto;
import com.erp.modules.procurement.entity.Item;
import com.erp.modules.procurement.entity.POStatus;
import com.erp.modules.procurement.repository.ItemRepository;
import com.erp.modules.procurement.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanningService {

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");
    private static final List<WOStatus> OPEN_WORK_ORDER_STATUSES = List.of(WOStatus.PLANNED, WOStatus.RELEASED, WOStatus.IN_PROGRESS);
    private static final List<WOStatus> CLOSED_WORK_ORDER_STATUSES = List.of(WOStatus.CLOSED, WOStatus.CANCELLED);
    private static final List<POStatus> OPEN_PO_STATUSES = List.of(POStatus.PENDING_APPROVAL, POStatus.APPROVED, POStatus.SENT, POStatus.PARTIALLY_RECEIVED);

    private final StockRepository stockRepository;
    private final WarehouseRepository warehouseRepository;
    private final ItemRepository itemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderRequirementRepository workOrderRequirementRepository;

    public InventoryFilterDto createInventoryFilter(String search, String category, String warehouse,
                                                    String status, boolean criticalOnly) {
        InventoryFilterDto filter = new InventoryFilterDto();
        filter.setSearch(trimToNull(search));
        filter.setCategory(trimToNull(category));
        filter.setWarehouse(trimToNull(warehouse));
        filter.setCriticalOnly(criticalOnly);
        filter.setStatus(parseInventoryStatus(status));
        return filter;
    }

    public List<InventoryItemDto> getInventoryItems(InventoryFilterDto filter) {
        return inventoryRows().stream()
            .filter(item -> matchesSearch(item, filter.getSearch()))
            .filter(item -> filter.getCategory() == null || item.getCategory().equals(filter.getCategory()))
            .filter(item -> filter.getWarehouse() == null || item.getWarehouse().equals(filter.getWarehouse()))
            .filter(item -> filter.getStatus() == null || item.getStatus() == filter.getStatus())
            .filter(item -> !filter.isCriticalOnly() || item.getStatus() == InventoryStatus.KRITIK || item.getStatus() == InventoryStatus.STOK_YOK)
            .sorted(Comparator.comparing(InventoryItemDto::getMaterialCode))
            .toList();
    }

    private List<InventoryItemDto> inventoryRows() {
        List<Stock> stocks = stockRepository.findAll();
        List<InventoryItemDto> rows = new ArrayList<>(stocks.stream().map(this::toInventoryItem).toList());
        String fallbackWarehouse = firstWarehouseName();

        itemRepository.findAll().stream()
            .filter(item -> stocks.stream().noneMatch(stock -> stock.getItem().getId().equals(item.getId())))
            .map(item -> toInventoryItemWithoutStock(item, fallbackWarehouse))
            .forEach(rows::add);

        return rows;
    }

    public List<SummaryCardDto> getInventorySummaryCards(List<InventoryItemDto> items) {
        BigDecimal totalValue = items.stream()
            .map(InventoryItemDto::getStockValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long criticalCount = items.stream()
            .filter(item -> item.getStatus() == InventoryStatus.KRITIK)
            .count();

        long outOfStockCount = items.stream()
            .filter(item -> item.getStatus() == InventoryStatus.STOK_YOK)
            .count();

        long activeWarehouses = items.stream()
            .map(InventoryItemDto::getWarehouse)
            .distinct()
            .count();

        return List.of(
            SummaryCardDto.builder().label("Toplam Stok Kalemi").value(String.valueOf(items.size())).icon("fas fa-boxes").variant("primary").build(),
            SummaryCardDto.builder().label("Kritik Stok").value(String.valueOf(criticalCount)).icon("fas fa-exclamation-triangle").variant("warning").build(),
            SummaryCardDto.builder().label("Stok Yok").value(String.valueOf(outOfStockCount)).icon("fas fa-ban").variant("danger").build(),
            SummaryCardDto.builder().label("Toplam Stok Değeri").value(formatCurrency(totalValue)).icon("fas fa-coins").variant("success").build(),
            SummaryCardDto.builder().label("Aktif Depo Sayısı").value(String.valueOf(activeWarehouses)).icon("fas fa-warehouse").variant("info").build()
        );
    }

    public List<WarehouseSummaryDto> getWarehouseSummaries(List<InventoryItemDto> items) {
        return items.stream()
            .collect(Collectors.groupingBy(InventoryItemDto::getWarehouse))
            .entrySet()
            .stream()
            .map(entry -> WarehouseSummaryDto.builder()
                .warehouse(entry.getKey())
                .itemCount(entry.getValue().size())
                .criticalItemCount(entry.getValue().stream()
                    .filter(item -> item.getStatus() == InventoryStatus.KRITIK || item.getStatus() == InventoryStatus.STOK_YOK)
                    .count())
                .totalStockValue(entry.getValue().stream()
                    .map(InventoryItemDto::getStockValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build())
            .toList();
    }

    public MrpRequestDto defaultMrpRequest() {
        MrpRequestDto request = new MrpRequestDto();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now().plusDays(30));
        request.setProductGroup("Tüm Ürün Grupları");
        request.setWarehouse(firstWarehouseName());
        request.setIncludeSafetyStock(true);
        request.setIncludeOpenPurchaseOrders(true);
        request.setIncludeOpenWorkOrders(true);
        return request;
    }

    public MrpRequestDto normalizeMrpRequest(MrpRequestDto request) {
        MrpRequestDto normalized = request != null ? request : defaultMrpRequest();
        if (normalized.getStartDate() == null) {
            normalized.setStartDate(LocalDate.now());
        }
        if (normalized.getEndDate() == null) {
            normalized.setEndDate(normalized.getStartDate().plusDays(30));
        }
        if (trimToNull(normalized.getProductGroup()) == null) {
            normalized.setProductGroup("Tüm Ürün Grupları");
        }
        if (trimToNull(normalized.getWarehouse()) == null) {
            normalized.setWarehouse(firstWarehouseName());
        }
        return normalized;
    }

    public List<MrpResultDto> runMrp(MrpRequestDto request) {
        MrpRequestDto normalized = normalizeMrpRequest(request);
        LocalDateTime horizonDate = normalized.getEndDate().atTime(23, 59, 59);
        List<WorkOrderRequirement> requirements = workOrderRequirementRepository.findRequirementsWithinHorizon(horizonDate, CLOSED_WORK_ORDER_STATUSES);
        Map<Long, BigDecimal> demandByItem = requirements.stream()
            .collect(Collectors.groupingBy(req -> req.getItem().getId(),
                Collectors.reducing(BigDecimal.ZERO, WorkOrderRequirement::getRequiredQuantity, BigDecimal::add)));

        Map<Long, BigDecimal> openPurchaseByItem = normalized.isIncludeOpenPurchaseOrders()
            ? openPurchaseQuantitiesByItem()
            : Map.of();

        Map<Long, BigDecimal> openWorkOrderByItem = normalized.isIncludeOpenWorkOrders()
            ? openWorkOrderQuantitiesByItem()
            : Map.of();

        String productGroup = normalized.getProductGroup();
        return itemRepository.findAll().stream()
            .filter(item -> "Tüm Ürün Grupları".equals(productGroup) || deriveCategory(item).equals(productGroup))
            .map(item -> toMrpResult(item, normalized, demandByItem, openPurchaseByItem, openWorkOrderByItem))
            .filter(result -> result.getNetRequirement().compareTo(BigDecimal.ZERO) > 0 || result.getStatus() != MrpStatus.NORMAL)
            .sorted(Comparator.comparing(MrpResultDto::getStatus).thenComparing(MrpResultDto::getMaterialCode))
            .toList();
    }

    public List<SummaryCardDto> getMrpSummaryCards(List<MrpResultDto> results) {
        BigDecimal totalNetRequirement = results.stream()
            .map(MrpResultDto::getNetRequirement)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long purchasingSuggestions = results.stream()
            .filter(result -> result.getActionType() == MrpActionType.SATIN_ALMA_OLUSTUR)
            .count();

        long workOrderSuggestions = results.stream()
            .filter(result -> result.getActionType() == MrpActionType.URETIM_EMRI_OLUSTUR)
            .count();

        long criticalMaterials = results.stream()
            .filter(result -> result.getStatus() == MrpStatus.ACIL || result.getStatus() == MrpStatus.KRITIK)
            .count();

        BigDecimal estimatedCost = results.stream()
            .map(MrpResultDto::getEstimatedCost)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return List.of(
            SummaryCardDto.builder().label("Toplam Net İhtiyaç").value(formatQuantity(totalNetRequirement)).icon("fas fa-calculator").variant("primary").build(),
            SummaryCardDto.builder().label("Satın Alma Önerisi").value(String.valueOf(purchasingSuggestions)).icon("fas fa-file-invoice-dollar").variant("success").build(),
            SummaryCardDto.builder().label("Üretim Emri Önerisi").value(String.valueOf(workOrderSuggestions)).icon("fas fa-industry").variant("info").build(),
            SummaryCardDto.builder().label("Kritik Malzeme").value(String.valueOf(criticalMaterials)).icon("fas fa-exclamation-circle").variant("warning").build(),
            SummaryCardDto.builder().label("Tahmini Toplam Maliyet").value(formatCurrency(estimatedCost)).icon("fas fa-coins").variant("success").build()
        );
    }

    public String createRunId() {
        return "MRP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(TR_LOCALE);
    }

    public List<String> getCategories() {
        return itemRepository.findAll().stream()
            .map(this::deriveCategory)
            .distinct()
            .sorted()
            .toList();
    }

    public List<String> getWarehouses() {
        return warehouseRepository.findAll().stream()
            .map(Warehouse::getName)
            .sorted()
            .toList();
    }

    public List<String> getProductGroups() {
        List<String> categories = getCategories();
        return java.util.stream.Stream.concat(java.util.stream.Stream.of("Tüm Ürün Grupları"), categories.stream()).toList();
    }

    public List<InventoryStatus> getInventoryStatuses() {
        return Arrays.asList(InventoryStatus.values());
    }

    public List<String> getItemUnits() {
        return List.of("Adet", "Kg", "Lt", "Metre", "Set");
    }

    @Transactional
    public Item createItem(ItemCreateRequest request) {
        String code = normalizeCode(request.getCode());
        if (itemRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Bu malzeme kodu zaten kullanılıyor.");
        }

        Item item = new Item();
        item.setCode(code);
        item.setName(trimToNull(request.getName()));
        item.setDescription(trimToNull(request.getDescription()));
        item.setPrice(request.getPrice());
        item.setUom(trimToNull(request.getUom()));
        return itemRepository.save(item);
    }

    private InventoryItemDto toInventoryItem(Stock stock) {
        Item item = stock.getItem();
        BigDecimal currentStock = BigDecimal.valueOf(stock.getOnHand());
        BigDecimal minimumStock = deriveMinimumStock(item);
        BigDecimal maximumStock = minimumStock.multiply(BigDecimal.valueOf(3));

        return InventoryItemDto.builder()
            .materialCode(item.getCode())
            .materialName(item.getName())
            .category(deriveCategory(item))
            .warehouse(stock.getWarehouse().getName())
            .unit(item.getUom())
            .currentStock(currentStock)
            .minimumStock(minimumStock)
            .maximumStock(maximumStock)
            .unitCost(safePrice(item))
            .status(determineInventoryStatus(currentStock, minimumStock, maximumStock))
            .lastMovementDate(stock.getUpdatedAt() != null ? stock.getUpdatedAt().toLocalDate() : LocalDate.now())
            .build();
    }

    private InventoryItemDto toInventoryItemWithoutStock(Item item, String warehouseName) {
        BigDecimal currentStock = BigDecimal.ZERO;
        BigDecimal minimumStock = deriveMinimumStock(item);
        BigDecimal maximumStock = minimumStock.multiply(BigDecimal.valueOf(3));

        return InventoryItemDto.builder()
            .materialCode(item.getCode())
            .materialName(item.getName())
            .category(deriveCategory(item))
            .warehouse(warehouseName)
            .unit(item.getUom())
            .currentStock(currentStock)
            .minimumStock(minimumStock)
            .maximumStock(maximumStock)
            .unitCost(safePrice(item))
            .status(determineInventoryStatus(currentStock, minimumStock, maximumStock))
            .lastMovementDate(item.getUpdatedAt() != null ? item.getUpdatedAt().toLocalDate() : LocalDate.now())
            .build();
    }

    private MrpResultDto toMrpResult(Item item, MrpRequestDto request, Map<Long, BigDecimal> demandByItem,
                                     Map<Long, BigDecimal> openPurchaseByItem, Map<Long, BigDecimal> openWorkOrderByItem) {
        BigDecimal demand = demandByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
        BigDecimal safetyStock = request.isIncludeSafetyStock() ? deriveMinimumStock(item) : BigDecimal.ZERO;
        BigDecimal currentStock = currentStock(item, request.getWarehouse());
        BigDecimal totalStock = totalStock(item);
        BigDecimal openPurchase = openPurchaseByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
        BigDecimal openWorkOrder = openWorkOrderByItem.getOrDefault(item.getId(), BigDecimal.ZERO);
        BigDecimal netRequirement = demand.add(safetyStock).subtract(currentStock).subtract(openPurchase).subtract(openWorkOrder);
        if (netRequirement.compareTo(BigDecimal.ZERO) < 0) {
            netRequirement = BigDecimal.ZERO;
        }

        BigDecimal suggestedQuantity = netRequirement.setScale(0, RoundingMode.CEILING);
        BigDecimal otherWarehouseStock = totalStock.subtract(currentStock);
        MrpActionType actionType = determineActionType(item, netRequirement, otherWarehouseStock);
        MrpStatus status = determineMrpStatus(netRequirement, currentStock, safetyStock);

        return MrpResultDto.builder()
            .materialCode(item.getCode())
            .materialName(item.getName())
            .category(deriveCategory(item))
            .demand(demand)
            .currentStock(currentStock)
            .safetyStock(safetyStock)
            .openPurchaseOrders(openPurchase)
            .openWorkOrders(openWorkOrder)
            .netRequirement(netRequirement)
            .suggestedQuantity(suggestedQuantity)
            .suggestedDate(request.getStartDate().plusDays(status == MrpStatus.ACIL ? 2 : 7))
            .actionType(actionType)
            .status(status)
            .estimatedCost(suggestedQuantity.multiply(safePrice(item)))
            .build();
    }

    private Map<Long, BigDecimal> openPurchaseQuantitiesByItem() {
        return purchaseOrderRepository.findByStatusIn(OPEN_PO_STATUSES).stream()
            .flatMap(order -> order.getItems().stream())
            .collect(Collectors.groupingBy(line -> line.getItem().getId(),
                Collectors.reducing(BigDecimal.ZERO, line -> BigDecimal.valueOf(line.getQuantity()), BigDecimal::add)));
    }

    private Map<Long, BigDecimal> openWorkOrderQuantitiesByItem() {
        return workOrderRepository.findAll().stream()
            .filter(order -> OPEN_WORK_ORDER_STATUSES.contains(order.getStatus()))
            .collect(Collectors.groupingBy(order -> order.getItem().getId(),
                Collectors.reducing(BigDecimal.ZERO, WorkOrder::getQuantity, BigDecimal::add)));
    }

    private BigDecimal currentStock(Item item, String warehouseName) {
        return stockRepository.findAll().stream()
            .filter(stock -> stock.getItem().getId().equals(item.getId()))
            .filter(stock -> warehouseName == null || stock.getWarehouse().getName().equals(warehouseName))
            .map(stock -> BigDecimal.valueOf(stock.getOnHand()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalStock(Item item) {
        return stockRepository.findAll().stream()
            .filter(stock -> stock.getItem().getId().equals(item.getId()))
            .map(stock -> BigDecimal.valueOf(stock.getOnHand()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MrpActionType determineActionType(Item item, BigDecimal netRequirement, BigDecimal otherWarehouseStock) {
        if (netRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return MrpActionType.AKSIYON_YOK;
        }
        if (otherWarehouseStock.compareTo(netRequirement) >= 0) {
            return MrpActionType.TRANSFER_ONER;
        }
        if ("Yarı Mamul".equals(deriveCategory(item))) {
            return MrpActionType.URETIM_EMRI_OLUSTUR;
        }
        return MrpActionType.SATIN_ALMA_OLUSTUR;
    }

    private MrpStatus determineMrpStatus(BigDecimal netRequirement, BigDecimal currentStock, BigDecimal safetyStock) {
        if (netRequirement.compareTo(BigDecimal.ZERO) <= 0) {
            return MrpStatus.NORMAL;
        }
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return MrpStatus.ACIL;
        }
        if (currentStock.compareTo(safetyStock.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) <= 0) {
            return MrpStatus.KRITIK;
        }
        return MrpStatus.PLANLANMALI;
    }

    private InventoryStatus determineInventoryStatus(BigDecimal currentStock, BigDecimal minimumStock, BigDecimal maximumStock) {
        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return InventoryStatus.STOK_YOK;
        }
        if (currentStock.compareTo(minimumStock.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)) <= 0) {
            return InventoryStatus.KRITIK;
        }
        if (currentStock.compareTo(minimumStock) < 0) {
            return InventoryStatus.DUSUK;
        }
        if (currentStock.compareTo(maximumStock) > 0) {
            return InventoryStatus.FAZLA_STOK;
        }
        return InventoryStatus.YETERLI;
    }

    private BigDecimal deriveMinimumStock(Item item) {
        String unit = item.getUom() != null ? item.getUom().toUpperCase(TR_LOCALE) : "";
        if (unit.contains("KG")) {
            return BigDecimal.valueOf(250);
        }
        if (unit.contains("LT")) {
            return BigDecimal.valueOf(120);
        }
        return BigDecimal.valueOf(50);
    }

    private String deriveCategory(Item item) {
        String searchable = (item.getCode() + " " + item.getName() + " " + (item.getDescription() != null ? item.getDescription() : "")).toLowerCase(TR_LOCALE);
        if (searchable.contains("palet") || searchable.contains("ambalaj") || searchable.contains("casing")) {
            return "Ambalaj";
        }
        if (searchable.contains("yağ") || searchable.contains("epoksi") || searchable.contains("boya")) {
            return "Kimyasal";
        }
        if (searchable.contains("circuit") || searchable.contains("terminal") || searchable.contains("elektr")) {
            return "Elektrik Komponenti";
        }
        if (searchable.contains("prod") || searchable.contains("widget")) {
            return "Yarı Mamul";
        }
        return "Hammadde";
    }

    private String normalizeCode(String value) {
        String normalized = trimToNull(value);
        return normalized != null ? normalized.toUpperCase(TR_LOCALE) : null;
    }

    private boolean matchesSearch(InventoryItemDto item, String search) {
        if (search == null) {
            return true;
        }

        String normalizedSearch = normalize(search);
        return normalize(item.getMaterialCode()).contains(normalizedSearch)
            || normalize(item.getMaterialName()).contains(normalizedSearch);
    }

    private InventoryStatus parseInventoryStatus(String status) {
        if (trimToNull(status) == null) {
            return null;
        }

        try {
            return InventoryStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String firstWarehouseName() {
        return warehouseRepository.findAll().stream()
            .map(Warehouse::getName)
            .findFirst()
            .orElse("Ana Depo");
    }

    private BigDecimal safePrice(Item item) {
        return item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(TR_LOCALE);
    }

    private String formatCurrency(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(TR_LOCALE);
        return formatter.format(value);
    }

    private String formatQuantity(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(TR_LOCALE);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(value);
    }
}
