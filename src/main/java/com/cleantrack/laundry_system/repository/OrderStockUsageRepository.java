package com.cleantrack.laundry_system.repository;

import com.cleantrack.laundry_system.model.OrderStockUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStockUsageRepository extends JpaRepository<OrderStockUsage, Long> {

    boolean existsByOrderId(Long orderId);
}
