package com.erp.modules.production.repository;

import com.erp.modules.production.entity.WorkOrderMaterialRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkOrderMaterialRequirementRepository extends JpaRepository<WorkOrderMaterialRequirement, Long> {

    List<WorkOrderMaterialRequirement> findByWorkOrderOperationId(Long workOrderOperationId);

    List<WorkOrderMaterialRequirement> findByWorkOrderOperation_WorkOrderId(Long workOrderId);

    @Query("SELECT womr FROM WorkOrderMaterialRequirement womr WHERE womr.workOrderOperation.workOrder.id = :workOrderId")
    List<WorkOrderMaterialRequirement> findByWorkOrderId(@Param("workOrderId") Long workOrderId);
}
