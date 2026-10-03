package com.cleantrack.repository;

import com.cleantrack.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByTrackingId(String trackingId);
    List<Order> findByCustomerId(Long customerId);
    long countByStatus(String status);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT service_type FROM orders GROUP BY service_type ORDER BY COUNT(id) DESC LIMIT 1", nativeQuery = true)
    String findMostPopularService();
}
