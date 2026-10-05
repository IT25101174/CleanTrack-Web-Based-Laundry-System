package com.cleantrack.laundry_system.model;
import com.cleantrack.laundry_system.strategy.UsageStrategy;
import com.cleantrack.laundry_system.strategy.UsageStrategyFactory;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold;

    @Column(length = 50)
    private String category;

    @Column(length = 30)
    private String unit;

    @Column(length = 100)
    private String supplier;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private java.math.BigDecimal unitPrice;

    // Automatic-use rule, applied when an order reaches the washing (cleaning) stage.
    // usageBasis: MANUAL (or null) = never deducted automatically,
    //             PER_GARMENTS   = 1 unit for every usageAmount garments (rounded up),
    //             PER_ORDER      = usageAmount units for each order.
    // usageServices: comma-separated service types the rule applies to (WASH_ONLY, WASH_IRON, DRY_CLEAN).
    @Column(name = "usage_basis", length = 20)
    private String usageBasis;

    @Column(name = "usage_amount")
    private Integer usageAmount;

    @Column(name = "usage_services", length = 60)
    private String usageServices;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public InventoryItem() {
    }

    public InventoryItem(String itemName, Integer quantity, Integer lowStockThreshold, String category, String unit, String supplier, java.math.BigDecimal unitPrice) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.category = category;
        this.unit = unit;
        this.supplier = supplier;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    // True when the quantity has fallen below the safety threshold (not mapped to a column).
    public boolean isLowStock() {
        return quantity != null && lowStockThreshold != null && quantity < lowStockThreshold;
    }

    public String getUsageBasis() {
        return usageBasis;
    }

    public void setUsageBasis(String usageBasis) {
        this.usageBasis = usageBasis;
    }

    public Integer getUsageAmount() {
        return usageAmount;
    }

    public void setUsageAmount(Integer usageAmount) {
        this.usageAmount = usageAmount;
    }

    public String getUsageServices() {
        return usageServices;
    }

    public void setUsageServices(String usageServices) {
        this.usageServices = usageServices;
    }

    // True when the item has a complete automatic-use rule (not mapped to a column).
    public boolean isAutoUse() {
        return usageStrategy().isAutomatic()
                && usageAmount != null && usageAmount > 0
                && usageServices != null && !usageServices.isBlank();
    }

    // Strategy pattern: the item is the context and delegates the usage calculation to the strategy
    // that matches its usage basis (manual, per garments or per order).
    private UsageStrategy usageStrategy() {
        return UsageStrategyFactory.forBasis(usageBasis);
    }

    // Does the rule apply to this service type? A missing or unknown service type counts as WASH_ONLY,
    // the same default the work queue uses.
    public boolean appliesTo(String serviceType) {
        if (usageServices == null || usageServices.isBlank()) {
            return false;
        }
        String type = serviceType == null ? "" : serviceType.trim().toUpperCase();
        if (!type.equals("WASH_IRON") && !type.equals("DRY_CLEAN")) {
            type = "WASH_ONLY";
        }
        for (String service : usageServices.split(",")) {
            if (service.trim().equals(type)) {
                return true;
            }
        }
        return false;
    }

    // Units to deduct for an order with this many garments.
    public int unitsFor(int garments) {
        if (!isAutoUse() || garments <= 0) {
            return 0;
        }
        return usageStrategy().unitsFor(garments, usageAmount);
    }

    // Short description shown on the inventory page, for example "1 per 20 garments (Wash Only, Wash & Iron)".
    public String getUsageSummary() {
        if (!isAutoUse()) {
            return null;
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String service : usageServices.split(",")) {
            switch (service.trim()) {
                case "WASH_ONLY": names.add("Wash Only"); break;
                case "WASH_IRON": names.add("Wash & Iron"); break;
                case "DRY_CLEAN": names.add("Dry Clean"); break;
                default: break;
            }
        }
        return usageStrategy().describe(usageAmount) + " (" + String.join(", ", names) + ")";
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public java.math.BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(java.math.BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
