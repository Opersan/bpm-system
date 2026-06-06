package com.erp.modules.production.repository;

import com.erp.modules.production.entity.Operation;
import com.erp.modules.production.entity.OperationMaterialRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationMaterialRequirementRepository extends JpaRepository<OperationMaterialRequirement, Long> {

    List<OperationMaterialRequirement> findByOperation(Operation operation);

    List<OperationMaterialRequirement> findByOperationId(Long operationId);

    long countByOperationId(Long operationId);
}
