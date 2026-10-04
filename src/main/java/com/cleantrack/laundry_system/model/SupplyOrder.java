package com.cleantrack.laundry_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "supply_orders")
public class SupplyOrder {

    public static final String PENDING = "PENDING";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The item and supplier are stored by id plus a copy of their name, so an order
    // keeps its history even if the item or supplier is later renamed or deleted.
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 20)
    private String status = PENDING;

    @Column(name = "ordered_by", length = 100)
    private String orderedBy;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt = LocalDateTime.now();

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(length = 200)
    private String notes;

    public SupplyOrder() {
    }

    public SupplyOrder(Long itemId, String itemName, Long supplierId, String supplierName,
                       Integer quantity, String orderedBy, String notes) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.quantity = quantity;
        this.orderedBy = orderedBy;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderedBy() {
        return orderedBy;
    }

    public void setOrderedBy(String orderedBy) {
        this.orderedBy = orderedBy;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Display helpers for the page. They are not database columns.
    public String getOrderedAtDisplay() {
        return orderedAt == null ? null : orderedAt.format(DISPLAY_FORMAT);
    }

    public String getReceivedAtDisplay() {
        return receivedAt == null ? null : receivedAt.format(DISPLAY_FORMAT);
    }
}