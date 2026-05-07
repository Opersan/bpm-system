package com.erp.modules.production.dto;

import com.erp.modules.production.model.WorkOrderPriority;
import com.erp.modules.production.model.WorkOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderFilterDto {
    private String search;
    private WorkOrderStatus status;
    private WorkOrderPriority priority;
    private String line;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private boolean delayedOnly;
}