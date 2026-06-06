package com.erp.modules.production.repository;

import com.erp.modules.production.entity.WorkOrderOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkOrderOperationRepository extends JpaRepository<WorkOrderOperation, Long> {

    List<WorkOrderOperation> findByWorkOrderId(Long workOrderId);

    List<WorkOrderOperation> findByWorkOrderIdOrderBySequenceNumberAsc(Long workOrderId);

    long countByWorkOrderId(Long workOrderId);
}
