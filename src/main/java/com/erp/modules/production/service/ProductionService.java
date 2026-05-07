package com.erp.modules.production.service;

import com.erp.modules.production.dto.ProductDto;
import com.erp.modules.production.dto.ProductionLineDto;
import com.erp.modules.production.dto.ProductionSummaryDto;
import com.erp.modules.production.dto.WorkOrderCreateRequest;
import com.erp.modules.production.dto.WorkOrderDto;
import com.erp.modules.production.dto.WorkOrderFilterDto;
import com.erp.modules.production.dto.WorkOrderMaterialRequirementDto;
import com.erp.modules.production.mock.ProductionMockData;
import com.erp.modules.production.model.MaterialRequirementStatus;
import com.erp.modules.production.model.WorkOrderPriority;
import com.erp.modules.production.model.WorkOrderStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductionService {

    private static final Locale TR_LOCALE = Locale.forLanguageTag("tr-TR");

    private final Map<String, ProductDto> productsByCode;
    private final Map<String, ProductionLineDto> linesByCode;
    private final Map<String, List<WorkOrderMaterialRequirementDto>> materialTemplates;
    private final List<String> responsibleUsers;
    private final List<String> shifts;
    private final Map<String, WorkOrderDto> workOrdersByNo = new ConcurrentHashMap<>();

    public ProductionService() {
        this.productsByCode = ProductionMockData.products().stream()
            .collect(Collectors.toMap(ProductDto::getCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        this.linesByCode = ProductionMockData.productionLines().stream()
            .collect(Collectors.toMap(ProductionLineDto::getCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        this.materialTemplates = new LinkedHashMap<>(ProductionMockData.bomTemplates());
        this.responsibleUsers = List.copyOf(ProductionMockData.responsibleUsers());
        this.shifts = List.copyOf(ProductionMockData.shifts());
        seedWorkOrders();
    }

    public WorkOrderFilterDto createFilter(String search, String status, String priority, String line,
                                           LocalDate startDate, LocalDate endDate, boolean delayedOnly) {
        return WorkOrderFilterDto.builder()
            .search(trimToNull(search))
            .status(parseStatus(status))
            .priority(parsePriority(priority))
            .line(trimToNull(line))
            .startDate(startDate)
            .endDate(endDate)
            .delayedOnly(delayedOnly)
            .build();
    }

    public List<WorkOrderDto> getFilteredWorkOrders(WorkOrderFilterDto filter) {
        WorkOrderFilterDto activeFilter = filter != null ? filter : new WorkOrderFilterDto();
        return workOrdersByNo.values().stream()
            .map(this::toViewModel)
            .filter(order -> matchesSearch(order, activeFilter.getSearch()))
            .filter(order -> matchesStatus(order, activeFilter.getStatus()))
            .filter(order -> activeFilter.getPriority() == null || activeFilter.getPriority() == order.getPriority())
            .filter(order -> activeFilter.getLine() == null || (order.getProductionLine() != null && activeFilter.getLine().equals(order.getProductionLine().getName())))
            .filter(order -> activeFilter.getStartDate() == null || !safeDate(order.getPlannedStartDate()).isBefore(activeFilter.getStartDate()))
            .filter(order -> activeFilter.getEndDate() == null || !safeDate(order.getPlannedEndDate()).isAfter(activeFilter.getEndDate()))
            .filter(order -> !activeFilter.isDelayedOnly() || order.isDelayed())
            .sorted(workOrderComparator())
            .toList();
    }

    public List<ProductionSummaryDto> getSummaryCards(List<WorkOrderDto> workOrders) {
        List<WorkOrderDto> source = workOrders != null ? workOrders : List.of();
        LocalDate today = LocalDate.now();
        long activeCount = source.stream()
            .filter(order -> order.getStatus() == WorkOrderStatus.PLANNED
                || order.getStatus() == WorkOrderStatus.IN_PROGRESS
                || order.getStatus() == WorkOrderStatus.PAUSED
                || order.getStatus() == WorkOrderStatus.DELAYED)
            .count();
        long delayedCount = source.stream().filter(WorkOrderDto::isDelayed).count();
        long todayStartCount = source.stream().filter(order -> today.equals(order.getPlannedStartDate())).count();
        long completedCount = source.stream().filter(order -> order.getStatus() == WorkOrderStatus.COMPLETED).count();
        long pausedCount = source.stream().filter(order -> order.getStatus() == WorkOrderStatus.PAUSED).count();

        return List.of(
            ProductionSummaryDto.builder().label("Toplam İş Emri").value(String.valueOf(source.size())).icon("fas fa-industry").variant("primary").build(),
            ProductionSummaryDto.builder().label("Aktif İş Emirleri").value(String.valueOf(activeCount)).icon("fas fa-play-circle").variant("info").build(),
            ProductionSummaryDto.builder().label("Geciken İş Emirleri").value(String.valueOf(delayedCount)).icon("fas fa-triangle-exclamation").variant("danger").build(),
            ProductionSummaryDto.builder().label("Bugün Başlayacaklar").value(String.valueOf(todayStartCount)).icon("fas fa-calendar-day").variant("warning").build(),
            ProductionSummaryDto.builder().label("Tamamlananlar").value(String.valueOf(completedCount)).icon("fas fa-circle-check").variant("success").build(),
            ProductionSummaryDto.builder().label("Duruşta Olanlar").value(String.valueOf(pausedCount)).icon("fas fa-pause-circle").variant("warning").build()
        );
    }

    public List<WorkOrderStatus> getStatuses() {
        return List.of(WorkOrderStatus.values());
    }

    public List<WorkOrderPriority> getPriorities() {
        return List.of(WorkOrderPriority.values());
    }

    public List<ProductDto> getProducts() {
        return productsByCode.values().stream().map(ProductDto::toBuilder).map(ProductDto.ProductDtoBuilder::build).toList();
    }

    public List<ProductionLineDto> getProductionLines() {
        return linesByCode.values().stream().map(ProductionLineDto::toBuilder).map(ProductionLineDto.ProductionLineDtoBuilder::build).toList();
    }

    public List<String> getResponsibleUsers() {
        return responsibleUsers;
    }

    public List<String> getShifts() {
        return shifts;
    }

    public Map<String, List<WorkOrderMaterialRequirementDto>> getMaterialTemplates() {
        return materialTemplates.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(item -> item.toBuilder().build()).toList(),
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    public WorkOrderCreateRequest defaultCreateRequest() {
        return WorkOrderCreateRequest.builder()
            .plannedQuantity(BigDecimal.ONE)
            .priority(WorkOrderPriority.NORMAL)
            .plannedStartDate(LocalDate.now().plusDays(1))
            .plannedEndDate(LocalDate.now().plusDays(5))
            .shift(shifts.getFirst())
            .build();
    }

    public WorkOrderCreateRequest prepareCreateRequest(WorkOrderCreateRequest request) {
        WorkOrderCreateRequest prepared = (request != null ? request : defaultCreateRequest()).toBuilder().build();
        if (prepared.getPlannedQuantity() == null) {
            prepared.setPlannedQuantity(BigDecimal.ONE);
        }
        if (prepared.getPriority() == null) {
            prepared.setPriority(WorkOrderPriority.NORMAL);
        }
        if (prepared.getPlannedStartDate() == null) {
            prepared.setPlannedStartDate(LocalDate.now().plusDays(1));
        }
        if (prepared.getPlannedEndDate() == null) {
            prepared.setPlannedEndDate(prepared.getPlannedStartDate().plusDays(4));
        }
        if (trimToNull(prepared.getShift()) == null) {
            prepared.setShift(shifts.getFirst());
        }
        ProductDto product = getSelectedProduct(prepared.getProductCode());
        if (product != null) {
            prepared.setUnit(product.getDefaultUnit());
        }
        return prepared;
    }

    public ProductDto getSelectedProduct(String productCode) {
        String key = trimToNull(productCode);
        return key == null ? null : productsByCode.get(key);
    }

    public ProductionLineDto getSelectedProductionLine(String lineCode) {
        String key = trimToNull(lineCode);
        return key == null ? null : linesByCode.get(key);
    }

    public List<WorkOrderMaterialRequirementDto> calculateMaterialRequirements(String productCode, BigDecimal plannedQuantity) {
        String code = trimToNull(productCode);
        if (code == null || !materialTemplates.containsKey(code)) {
            return List.of();
        }

        BigDecimal effectiveQuantity = plannedQuantity != null && plannedQuantity.compareTo(BigDecimal.ZERO) > 0
            ? plannedQuantity
            : BigDecimal.ONE;

        return materialTemplates.get(code).stream()
            .map(template -> calculateMaterialRequirement(template, effectiveQuantity))
            .toList();
    }

    public Map<String, Object> createFormSummary(WorkOrderCreateRequest request) {
        WorkOrderCreateRequest prepared = prepareCreateRequest(request);
        ProductDto product = getSelectedProduct(prepared.getProductCode());
        ProductionLineDto line = getSelectedProductionLine(prepared.getProductionLineCode());
        List<WorkOrderMaterialRequirementDto> materials = calculateMaterialRequirements(prepared.getProductCode(), prepared.getPlannedQuantity());
        BigDecimal estimatedDurationHours = estimateDurationHours(product, prepared.getPlannedQuantity(), line);
        MaterialRequirementStatus availabilityStatus = summarizeMaterialAvailability(materials);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("product", product != null ? product.getName() : "-");
        summary.put("quantity", prepared.getPlannedQuantity() != null ? formatQuantity(prepared.getPlannedQuantity()) + " " + nullSafeUnit(prepared.getUnit()) : "-");
        summary.put("line", line != null ? line.getName() : "-");
        summary.put("start", formatDate(prepared.getPlannedStartDate()));
        summary.put("end", formatDate(prepared.getPlannedEndDate()));
        summary.put("estimatedDuration", estimatedDurationHours.compareTo(BigDecimal.ZERO) > 0 ? formatQuantity(estimatedDurationHours) + " saat" : "-");
        summary.put("materialAvailability", availabilityStatus.getLabel());
        summary.put("materialAvailabilityCssClass", availabilityStatus.getCssClass());
        summary.put("hasMaterialShortage", materials.stream().anyMatch(WorkOrderMaterialRequirementDto::isShortage));
        summary.put("materialRiskCount", materials.stream().filter(WorkOrderMaterialRequirementDto::isAttentionRequired).count());
        return summary;
    }

    public WorkOrderDto createWorkOrder(WorkOrderCreateRequest request, boolean draft) {
        WorkOrderCreateRequest prepared = prepareCreateRequest(request);
        ProductDto product = requireProduct(prepared.getProductCode());
        ProductionLineDto line = requireLine(prepared.getProductionLineCode());
        WorkOrderDto workOrder = WorkOrderDto.builder()
            .workOrderNo(generateWorkOrderNo())
            .product(product.toBuilder().build())
            .plannedQuantity(prepared.getPlannedQuantity())
            .producedQuantity(BigDecimal.ZERO)
            .unit(product.getDefaultUnit())
            .productionLine(line.toBuilder().build())
            .priority(prepared.getPriority())
            .status(draft ? WorkOrderStatus.DRAFT : WorkOrderStatus.PLANNED)
            .plannedStartDate(prepared.getPlannedStartDate())
            .plannedEndDate(prepared.getPlannedEndDate())
            .responsible(prepared.getResponsible())
            .shift(prepared.getShift())
            .description(trimToNull(prepared.getDescription()))
            .materialRequirements(new ArrayList<>(calculateMaterialRequirements(product.getCode(), prepared.getPlannedQuantity())))
            .build();

        WorkOrderDto stored = toStoredWorkOrder(workOrder);
        workOrdersByNo.put(stored.getWorkOrderNo(), stored);
        return toViewModel(stored);
    }

    public String startWorkOrder(String workOrderNo) {
        WorkOrderDto workOrder = requireWorkOrder(workOrderNo);
        if (!workOrder.isStartAllowed()) {
            throw new IllegalStateException("Bu iş emri başlatılamaz.");
        }
        workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);
        return workOrderNo + " başlatıldı.";
    }

    public String pauseWorkOrder(String workOrderNo) {
        WorkOrderDto workOrder = requireWorkOrder(workOrderNo);
        if (!workOrder.isPauseAllowed()) {
            throw new IllegalStateException("Bu iş emri durdurulamaz.");
        }
        workOrder.setStatus(WorkOrderStatus.PAUSED);
        return workOrderNo + " durduruldu.";
    }

    public String completeWorkOrder(String workOrderNo) {
        WorkOrderDto workOrder = requireWorkOrder(workOrderNo);
        if (!workOrder.isCompleteAllowed()) {
            throw new IllegalStateException("Bu iş emri tamamlanamaz.");
        }
        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setProducedQuantity(workOrder.getPlannedQuantity());
        return workOrderNo + " tamamlandı.";
    }

    public String cancelWorkOrder(String workOrderNo) {
        WorkOrderDto workOrder = requireWorkOrder(workOrderNo);
        if (!workOrder.isCancelAllowed()) {
            throw new IllegalStateException("Bu iş emri iptal edilemez.");
        }
        workOrder.setStatus(WorkOrderStatus.CANCELLED);
        return workOrderNo + " iptal edildi.";
    }

    public Optional<WorkOrderDto> findWorkOrder(String workOrderNo) {
        return Optional.ofNullable(workOrdersByNo.get(workOrderNo)).map(this::toViewModel);
    }

    private void seedWorkOrders() {
        ProductionMockData.workOrders().stream()
            .map(this::toStoredWorkOrder)
            .forEach(order -> workOrdersByNo.put(order.getWorkOrderNo(), order));
    }

    private WorkOrderDto toStoredWorkOrder(WorkOrderDto source) {
        WorkOrderDto stored = source.toBuilder().build();
        ProductDto product = requireProduct(stored.getProduct().getCode());
        ProductionLineDto line = requireLine(stored.getProductionLine().getCode());
        stored.setProduct(product.toBuilder().build());
        stored.setProductionLine(line.toBuilder().build());
        stored.setUnit(product.getDefaultUnit());
        stored.setMaterialRequirements(new ArrayList<>(calculateMaterialRequirements(product.getCode(), stored.getPlannedQuantity())));
        stored.setCompletionRate(calculateCompletionRate(stored.getPlannedQuantity(), stored.getProducedQuantity()));
        stored.setDelayed(isWorkOrderDelayed(stored));
        return stored;
    }

    private WorkOrderDto toViewModel(WorkOrderDto source) {
        WorkOrderDto viewModel = source.toBuilder().build();
        viewModel.setMaterialRequirements(source.getMaterialRequirements().stream()
            .map(requirement -> requirement.toBuilder().build())
            .toList());
        viewModel.setCompletionRate(calculateCompletionRate(viewModel.getPlannedQuantity(), viewModel.getProducedQuantity()));
        viewModel.setDelayed(isWorkOrderDelayed(viewModel));
        return viewModel;
    }

    private WorkOrderMaterialRequirementDto calculateMaterialRequirement(WorkOrderMaterialRequirementDto template, BigDecimal quantity) {
        BigDecimal requiredQuantity = safeDecimal(template.getBaseQuantity()).multiply(quantity).setScale(2, RoundingMode.HALF_UP);
        BigDecimal availableStock = safeDecimal(template.getAvailableStock()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shortageQuantity = requiredQuantity.subtract(availableStock).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAfterAllocation = availableStock.subtract(requiredQuantity);

        MaterialRequirementStatus status;
        if (shortageQuantity.compareTo(BigDecimal.ZERO) > 0) {
            status = availableStock.compareTo(BigDecimal.ZERO) == 0 || shortageQuantity.compareTo(requiredQuantity.multiply(BigDecimal.valueOf(0.5))) >= 0
                ? MaterialRequirementStatus.CRITICAL
                : MaterialRequirementStatus.MISSING;
        } else if (remainingAfterAllocation.compareTo(requiredQuantity.multiply(BigDecimal.valueOf(0.15)).negate()) >= 0
            && remainingAfterAllocation.compareTo(requiredQuantity.multiply(BigDecimal.valueOf(0.20))) <= 0) {
            status = MaterialRequirementStatus.LOW_STOCK;
        } else {
            status = MaterialRequirementStatus.AVAILABLE;
        }

        return template.toBuilder()
            .requiredQuantity(requiredQuantity)
            .availableStock(availableStock)
            .shortageQuantity(shortageQuantity)
            .status(status)
            .build();
    }

    private MaterialRequirementStatus summarizeMaterialAvailability(List<WorkOrderMaterialRequirementDto> materials) {
        if (materials == null || materials.isEmpty()) {
            return MaterialRequirementStatus.AVAILABLE;
        }
        if (materials.stream().anyMatch(item -> item.getStatus() == MaterialRequirementStatus.CRITICAL)) {
            return MaterialRequirementStatus.CRITICAL;
        }
        if (materials.stream().anyMatch(item -> item.getStatus() == MaterialRequirementStatus.MISSING)) {
            return MaterialRequirementStatus.MISSING;
        }
        if (materials.stream().anyMatch(item -> item.getStatus() == MaterialRequirementStatus.LOW_STOCK)) {
            return MaterialRequirementStatus.LOW_STOCK;
        }
        return MaterialRequirementStatus.AVAILABLE;
    }

    private BigDecimal estimateDurationHours(ProductDto product, BigDecimal quantity, ProductionLineDto line) {
        if (product == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseDuration = safeDecimal(product.getStandardProductionHours()).multiply(quantity);
        if (line == null) {
            return baseDuration.setScale(1, RoundingMode.HALF_UP);
        }

        BigDecimal factor = switch (line.getStatus()) {
            case AVAILABLE -> BigDecimal.ONE;
            case BUSY -> BigDecimal.valueOf(1.15);
            case MAINTENANCE -> BigDecimal.valueOf(1.30);
            case OFFLINE -> BigDecimal.valueOf(1.45);
        };
        return baseDuration.multiply(factor).setScale(1, RoundingMode.HALF_UP);
    }

    private int calculateCompletionRate(BigDecimal plannedQuantity, BigDecimal producedQuantity) {
        if (plannedQuantity == null || plannedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        BigDecimal rate = safeDecimal(producedQuantity)
            .multiply(BigDecimal.valueOf(100))
            .divide(plannedQuantity, 0, RoundingMode.HALF_UP);
        return Math.max(0, Math.min(100, rate.intValue()));
    }

    private boolean isWorkOrderDelayed(WorkOrderDto workOrder) {
        if (workOrder.getStatus() == WorkOrderStatus.DELAYED) {
            return true;
        }
        if (workOrder.getStatus() == null || workOrder.getStatus().isTerminal() || workOrder.getPlannedEndDate() == null) {
            return false;
        }
        return workOrder.getPlannedEndDate().isBefore(LocalDate.now());
    }

    private String generateWorkOrderNo() {
        int nextIndex = workOrdersByNo.keySet().stream()
            .map(this::extractSequence)
            .max(Integer::compareTo)
            .orElse(0) + 1;
        return "WO-2026-" + String.format(TR_LOCALE, "%04d", nextIndex);
    }

    private int extractSequence(String workOrderNo) {
        if (workOrderNo == null || !workOrderNo.contains("-")) {
            return 0;
        }
        String[] tokens = workOrderNo.split("-");
        try {
            return Integer.parseInt(tokens[tokens.length - 1]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private Comparator<WorkOrderDto> workOrderComparator() {
        return Comparator
            .comparingInt((WorkOrderDto order) -> order.isDelayed() ? 1 : 0)
            .reversed()
            .thenComparing(Comparator.comparingInt((WorkOrderDto order) -> order.getPriority() != null ? order.getPriority().getSortOrder() : 0).reversed())
            .thenComparing(WorkOrderDto::getPlannedStartDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(WorkOrderDto::getWorkOrderNo, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean matchesSearch(WorkOrderDto order, String search) {
        if (search == null) {
            return true;
        }
        String lowered = search.toLowerCase(TR_LOCALE);
        return contains(order.getWorkOrderNo(), lowered)
            || (order.getProduct() != null && contains(order.getProduct().getCode(), lowered))
            || (order.getProduct() != null && contains(order.getProduct().getName(), lowered));
    }

    private boolean matchesStatus(WorkOrderDto order, WorkOrderStatus status) {
        if (status == null) {
            return true;
        }
        if (status == WorkOrderStatus.DELAYED) {
            return order.isDelayed();
        }
        return status == order.getStatus();
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(TR_LOCALE).contains(search);
    }

    private ProductDto requireProduct(String productCode) {
        return Optional.ofNullable(getSelectedProduct(productCode))
            .orElseThrow(() -> new IllegalArgumentException("Geçerli bir ürün seçiniz."));
    }

    private ProductionLineDto requireLine(String lineCode) {
        return Optional.ofNullable(getSelectedProductionLine(lineCode))
            .orElseThrow(() -> new IllegalArgumentException("Geçerli bir üretim hattı seçiniz."));
    }

    private WorkOrderDto requireWorkOrder(String workOrderNo) {
        return Optional.ofNullable(workOrdersByNo.get(workOrderNo))
            .orElseThrow(() -> new IllegalArgumentException("İş emri bulunamadı: " + workOrderNo));
    }

    private WorkOrderStatus parseStatus(String value) {
        try {
            return value == null || value.isBlank() ? null : WorkOrderStatus.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private WorkOrderPriority parsePriority(String value) {
        try {
            return value == null || value.isBlank() ? null : WorkOrderPriority.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDate safeDate(LocalDate value) {
        return value != null ? value : LocalDate.now().plus(365, ChronoUnit.DAYS);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDate(LocalDate value) {
        return value != null ? value.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy", TR_LOCALE)) : "-";
    }

    private String formatQuantity(BigDecimal value) {
        NumberFormat formatter = NumberFormat.getNumberInstance(TR_LOCALE);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return formatter.format(Objects.requireNonNullElse(value, BigDecimal.ZERO));
    }

    private String nullSafeUnit(String value) {
        return value != null && !value.isBlank() ? value : "Adet";
    }
}