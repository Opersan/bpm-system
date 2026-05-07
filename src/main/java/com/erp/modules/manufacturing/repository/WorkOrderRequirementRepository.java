package com.erp.modules.manufacturing.repository;

import com.erp.modules.manufacturing.entity.WOStatus;
import com.erp.modules.manufacturing.entity.WorkOrderRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkOrderRequirementRepository extends JpaRepository<WorkOrderRequirement, Long> {
    
    @Query("SELECT r FROM WorkOrderRequirement r " +
           "JOIN r.workOrder w " +
           "WHERE w.status NOT IN :excludedStatuses " +
           "AND w.startDate <= :horizonDate")
    List<WorkOrderRequirement> findRequirementsWithinHorizon(@Param("horizonDate") LocalDateTime horizonDate,
                                                             @Param("excludedStatuses") List<WOStatus> excludedStatuses);

    default List<WorkOrderRequirement> findRequirementsWithinHorizon(LocalDateTime horizonDate) {
        return findRequirementsWithinHorizon(horizonDate, List.of(WOStatus.CLOSED, WOStatus.CANCELLED));
    }
}
