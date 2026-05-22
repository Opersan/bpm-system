package com.erp.modules.purchaserequest.repository;

import com.erp.modules.purchaserequest.entity.PurchaseRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRequestItemRepository extends JpaRepository<PurchaseRequestItem, Long> {
}
