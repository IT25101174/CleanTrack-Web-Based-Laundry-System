package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsBySupplierNameIgnoreCase(String supplierName);

    Optional<Supplier> findBySupplierNameIgnoreCase(String supplierName);

    List<Supplier> findAllByOrderBySupplierNameAsc();
}