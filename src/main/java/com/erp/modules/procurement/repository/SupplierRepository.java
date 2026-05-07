package com.erp.modules.procurement.repository;

import com.erp.modules.procurement.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	boolean existsByNameIgnoreCase(String name);
}
