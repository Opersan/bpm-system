package com.erp.modules.production.repository;

import com.erp.modules.production.entity.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    Optional<Operation> findByCode(String code);

    @Query("SELECT o FROM Operation o WHERE o.active = true ORDER BY o.defaultSequence, o.code")
    List<Operation> findActiveOperations();

    @Query("SELECT o FROM Operation o WHERE o.active = true AND o.id NOT IN :excludeIds ORDER BY o.defaultSequence, o.code")
    List<Operation> findActiveOperationsExcluding(@Param("excludeIds") List<Long> excludeIds);

    long countByCode(String code);
}
