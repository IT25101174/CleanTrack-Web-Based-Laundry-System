package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.SupplyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplyOrderRepository extends JpaRepository<SupplyOrder, Long> {

    List<SupplyOrder> findAllByOrderByOrderedAtDescIdDesc();

    long countByStatus(String status);
}