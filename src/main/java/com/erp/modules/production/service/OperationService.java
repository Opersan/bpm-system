package com.erp.modules.production.service;

import com.erp.modules.production.entity.Operation;
import com.erp.modules.production.entity.OperationMaterialRequirement;
import com.erp.modules.production.entity.WorkOrderOperation;
import com.erp.modules.production.repository.OperationMaterialRequirementRepository;
import com.erp.modules.production.repository.OperationRepository;
import com.erp.modules.production.repository.WorkOrderOperationRepository;
import com.erp.modules.production.dto.OperationCreateRequest;
import com.erp.modules.production.dto.OperationDto;
import com.erp.modules.production.dto.OperationFilterDto;
import com.erp.modules.production.dto.OperationMaterialRequirementDto;
import com.erp.modules.procurement.entity.Item;
import com.erp.modules.procurement.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;
    private final OperationMaterialRequirementRepository operationMaterialRequirementRepository;
    private final WorkOrderOperationRepository workOrderOperationRepository;
    private final ItemRepository itemRepository;

    public List<OperationDto> getOperations(OperationFilterDto filter) {
        List<Operation> operations;
        if (filter == null || filter.getSearch() == null || filter.getSearch().isBlank()) {
            if (filter != null && filter.getActive() != null) {
                operations = operationRepository.findAll();
            } else {
                operations = operationRepository.findAll();
            }
        } else {
            String searchPattern = "%" + filter.getSearch().toLowerCase().trim() + "%";
            operations = operationRepository.findAll();
        }

        return operations.stream()
            .map(this::toDto)
            .toList();
    }

    public List<OperationDto> getActiveOperations() {
        return operationRepository.findActiveOperations().stream()
            .map(this::toDto)
            .toList();
    }

    public OperationDto getOperation(Long id) {
        return operationRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Operasyon bulunamadı."));
    }

    public OperationDto getOperationByCode(String code) {
        return operationRepository.findByCode(code)
            .map(this::toDto)
            .orElseThrow(() -> new IllegalArgumentException("Operasyon bulunamadı."));
    }

    @Transactional
    public OperationDto createOperation(OperationCreateRequest request) {
        validateOperationCode(request.getCode(), null);

        Operation operation = new Operation();
        updateOperationFromRequest(operation, request);
        operation = operationRepository.save(operation);

        return toDto(operation);
    }

    @Transactional
    public OperationDto updateOperation(Long id, OperationCreateRequest request) {
        Operation operation = operationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Operasyon bulunamadı."));

        validateOperationCode(request.getCode(), id);

        updateOperationFromRequest(operation, request);
        operation = operationRepository.save(operation);

        return toDto(operation);
    }

    @Transactional
    public void deleteOperation(Long id) {
        if (workOrderOperationRepository.countByWorkOrderId(id) > 0) {
            throw new IllegalStateException("Bu operasyonu silmek için iş emri bağımlılığı vardır.");
        }
        operationRepository.deleteById(id);
    }

    @Transactional
    public void deactivateOperation(Long id) {
        Operation operation = operationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Operasyon bulunamadı."));
        operation.setActive(false);
        operationRepository.save(operation);
    }

    public List<OperationMaterialRequirementDto> getMaterialRequirements(Long operationId) {
        return operationMaterialRequirementRepository.findByOperationId(operationId).stream()
            .map(this::toMaterialRequirementDto)
            .toList();
    }

    private OperationDto toDto(Operation operation) {
        return OperationDto.builder()
            .id(operation.getId())
            .code(operation.getCode())
            .name(operation.getName())
            .description(operation.getDescription())
            .standardDuration(operation.getStandardDuration())
            .durationUnit(operation.getDurationUnit())
            .defaultSequence(operation.getDefaultSequence())
            .workCenter(operation.getWorkCenter())
            .capacity(operation.getCapacity())
            .capacityUnit(operation.getCapacityUnit())
            .active(operation.isActive())
            .materialRequirements(operation.getMaterialRequirements().stream()
                .map(this::toMaterialRequirementDto)
                .toList())
            .materialRequirementCount(operation.getMaterialRequirements().size())
            .build();
    }

    private OperationMaterialRequirementDto toMaterialRequirementDto(OperationMaterialRequirement requirement) {
        if (requirement.getItem() == null) {
            throw new IllegalStateException("OperationMaterialRequirement item is null for requirement id: " + requirement.getId());
        }
        return OperationMaterialRequirementDto.builder()
            .id(requirement.getId())
            .itemId(requirement.getItem().getId().toString())
            .itemName(requirement.getItem().getName())
            .unit(requirement.getUnit())
            .requiredQuantity(requirement.getRequiredQuantity())
            .scrapRate(requirement.getScrapRate())
            .critical(requirement.isCritical())
            .description(requirement.getDescription())
            .build();
    }

    private void updateOperationFromRequest(Operation operation, OperationCreateRequest request) {
        operation.setCode(request.getCode());
        operation.setName(request.getName());
        operation.setDescription(request.getDescription());
        operation.setStandardDuration(request.getStandardDuration());
        operation.setDurationUnit(request.getDurationUnit());
        operation.setDefaultSequence(request.getDefaultSequence());
        operation.setWorkCenter(request.getWorkCenter());
        operation.setCapacity(request.getCapacity());
        operation.setCapacityUnit(request.getCapacityUnit());
        operation.setActive(request.isActive());

        if (request.getMaterialRequirements() != null) {
            operation.getMaterialRequirements().clear();
            for (OperationMaterialRequirementDto dto : request.getMaterialRequirements()) {
                OperationMaterialRequirement requirement = new OperationMaterialRequirement();
                requirement.setOperation(operation);
                // Look up Item entity by ID
                Long itemId = parseItemId(dto.getItemId());
                Item item = itemRepository.findById(itemId).orElseThrow(() ->
                    new IllegalArgumentException("Malzeme bulunamadı: " + dto.getItemId()));
                requirement.setItem(item);
                requirement.setUnit(dto.getUnit());
                requirement.setRequiredQuantity(dto.getRequiredQuantity());
                requirement.setScrapRate(dto.getScrapRate());
                requirement.setCritical(dto.isCritical());
                requirement.setDescription(dto.getDescription());
                operation.getMaterialRequirements().add(requirement);
            }
        }
    }

    private Long parseItemId(String itemId) {
        try {
            return itemId != null && !itemId.isBlank() ? Long.parseLong(itemId) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateOperationCode(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Operasyon kodu zorunludur.");
        }

        long count = operationRepository.countByCode(code.toLowerCase().trim());
        if (excludeId != null) {
            Operation existing = operationRepository.findById(excludeId).orElse(null);
            if (existing != null && !existing.getCode().toLowerCase().trim().equals(code.toLowerCase().trim())) {
                if (count > 0) {
                    throw new IllegalArgumentException("Bu operasyon kodu zaten kullanılıyor.");
                }
            }
        } else {
            if (count > 0) {
                throw new IllegalArgumentException("Bu operasyon kodu zaten kullanılıyor.");
            }
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalMaterialQuantity(Long operationId, Long itemId, BigDecimal baseQuantity) {
        Operation operation = operationRepository.findById(operationId)
            .orElseThrow(() -> new IllegalArgumentException("Operasyon bulunamadı."));

        return operation.getMaterialRequirements().stream()
            .filter(mr -> mr.getItem().getId().equals(itemId))
            .map(mr -> {
                BigDecimal quantityPerUnit = mr.getRequiredQuantity();
                BigDecimal scrapFactor = BigDecimal.ONE;
                if (mr.getScrapRate() != null && mr.getScrapRate().compareTo(BigDecimal.ZERO) > 0) {
                    scrapFactor = BigDecimal.ONE.add(mr.getScrapRate().divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP));
                }
                return quantityPerUnit.multiply(baseQuantity).multiply(scrapFactor);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
