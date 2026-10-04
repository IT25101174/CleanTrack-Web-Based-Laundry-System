package com.cleantrack.laundry_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records that the automatic stock deduction has already been run for an order,
 * so a payment event can never deduct the same order's stock twice.
 */
@Entity
@Table(name = "order_stock_usage", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class OrderStockUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "deducted_at", nullable = false)
    private LocalDateTime deductedAt = LocalDateTime.now();

    public OrderStockUsage() {
    }

    public OrderStockUsage(Long orderId) {
        this.orderId = orderId;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public LocalDateTime getDeductedAt() {
        return deductedAt;
    }

    public void setDeductedAt(LocalDateTime deductedAt) {
        this.deductedAt = deductedAt;
    }
}
