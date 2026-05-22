package com.erp.modules.purchaserequest.repository;

import com.erp.modules.purchaserequest.entity.PurchaseRequest;
import com.erp.modules.purchaserequest.entity.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findByRequestedByOrderByCreatedAtDesc(String requestedBy);

    List<PurchaseRequest> findByStatusOrderByCreatedAtAsc(PurchaseRequestStatus status);

    List<PurchaseRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByRequestNumber(String requestNumber);
}
