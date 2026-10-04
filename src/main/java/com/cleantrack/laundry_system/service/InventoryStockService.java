package com.cleantrack.laundry_system.service;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.model.InventoryItem;
import com.cleantrack.laundry_system.model.Order;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import com.cleantrack.laundry_system.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central place for reducing stock. It is used by the manual "Use" action in the inventory page
 * and by the work queue, which deducts supplies automatically when an order reaches the washing
 * stage (UC-05, extension 3a).
 */
@Service
public class InventoryStockService {

    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public InventoryStockService(InventoryItemRepository inventoryItemRepository,
                                 AuditLogRepository auditLogRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /** Result of the automatic deduction for one order. */
    public static class AutoConsumeResult {
        private final List<String> deducted = new ArrayList<>();
        private final List<String> shortages = new ArrayList<>();

        public List<String> getDeducted() {
            return deducted;
        }

        public List<String> getShortages() {
            return shortages;
        }
    }

    /**
     * Reduces the stock of one item atomically.
     * Returns false when there is not enough stock (nothing is changed in that case).
     * When the deduction takes the item from at or above its threshold to below it,
     * a single low-stock alert is written to the audit log, so the alert is not repeated
     * for every later deduction.
     */
    public boolean consume(InventoryItem item, int amount, String reason, String actor) {
        int quantityBefore = item.getQuantity();
        int updated = inventoryItemRepository.removeStock(item.getId(), amount, LocalDateTime.now());
        if (updated == 0) {
            return false;
        }

        auditLogRepository.save(new AuditLog("Consumed " + amount + " units of " + item.getItemName()
                + " (" + reason + ") by " + actor));

        int quantityAfter = quantityBefore - amount;
        if (quantityBefore >= item.getLowStockThreshold() && quantityAfter < item.getLowStockThreshold()) {
            auditLogRepository.save(new AuditLog("LOW STOCK ALERT: " + item.getItemName() + " fell to "
                    + quantityAfter + " (threshold " + item.getLowStockThreshold() + ")"));
        }
        return true;
    }

    /**
     * Automatically deducts the supplies for an order that has just reached a processing stage
     * (washing, or cleaning for dry clean). Only items that have a usage rule applying to the order's
     * service type are deducted. The amount comes from the item's rule: either 1 unit per N garments
     * (rounded up) or a fixed number of units per order. A shortage never blocks the order: that item is
     * left unchanged and reported in the result and the audit log.
     */
    public AutoConsumeResult consumeForStage(Order order, String stageName, String actor) {
        AutoConsumeResult result = new AutoConsumeResult();
        int garments = order.getQuantity() == null ? 0 : order.getQuantity();
        if (garments <= 0) {
            return result;
        }

        String reason = "auto, " + stageName + " stage, order " + order.getTrackingId() + ", " + garments + " garments";
        for (InventoryItem item : inventoryItemRepository.findByUsageBasisIsNotNull()) {
            if (!item.isAutoUse() || !item.appliesTo(order.getServiceType())) {
                continue;
            }
            int needed = item.unitsFor(garments);
            if (needed <= 0) {
                continue;
            }

            if (consume(item, needed, reason, actor)) {
                result.getDeducted().add(item.getItemName() + " -" + needed);
            } else {
                result.getShortages().add(item.getItemName() + " (needed " + needed
                        + ", available " + item.getQuantity() + ")");
                auditLogRepository.save(new AuditLog("Automatic stock deduction skipped for order "
                        + order.getTrackingId() + ": not enough " + item.getItemName()));
            }
        }
        return result;
    }
}
