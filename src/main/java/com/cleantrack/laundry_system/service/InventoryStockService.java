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
 * Central place for reducing stock, used by the manual "Use" action and by the
 * automatic deduction that runs when an order is accepted (UC-05, extension 3a).
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

    /** Result of the automatic deduction for one accepted order. */
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
     * Automatically deducts stock for an accepted order. Every item with an auto-use rule
     * ("1 unit per N garments") is reduced by ceil(garments / N). A shortage never blocks
     * the order; it is returned so the caller can warn the user.
     */
    public AutoConsumeResult consumeForOrder(Order order, String actor) {
        AutoConsumeResult result = new AutoConsumeResult();
        Integer garments = order.getQuantity();
        if (garments == null || garments <= 0) {
            return result;
        }

        for (InventoryItem item : inventoryItemRepository.findByGarmentsPerUnitGreaterThan(0)) {
            int rule = item.getGarmentsPerUnit();
            int needed = (int) Math.ceil(garments / (double) rule);
            if (needed <= 0) {
                continue;
            }

            String reason = "auto, order " + order.getTrackingId() + ", " + garments + " garments";
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
