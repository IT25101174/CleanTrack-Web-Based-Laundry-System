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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central place for reducing stock. It is used by the manual "Use" action in the inventory page
 * and by the work queue, where the employee records the supplies used when an order reaches the
 * washing stage (UC-05, extension 3a).
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

    /** Result of recording the supplies used at a processing stage. */
    public static class StageUsageResult {
        private final String error;
        private final List<String> used;

        private StageUsageResult(String error, List<String> used) {
            this.error = error;
            this.used = used;
        }

        static StageUsageResult failure(String error) {
            return new StageUsageResult(error, new ArrayList<>());
        }

        static StageUsageResult success(List<String> used) {
            return new StageUsageResult(null, used);
        }

        public boolean isSuccess() {
            return error == null;
        }

        public String getError() {
            return error;
        }

        public List<String> getUsed() {
            return used;
        }
    }

    /** All inventory items, sorted by name (used to build the "supplies used" form). */
    public List<InventoryItem> listItems() {
        return inventoryItemRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryItem::getItemName, String.CASE_INSENSITIVE_ORDER))
                .toList();
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
     * Records the supplies the employee used when an order reaches a processing stage (washing or
     * cleaning). The employee enters a quantity for each item that was used; blank and zero entries are
     * ignored, and at least one item with a quantity above zero is required. Everything is checked first,
     * and if any deduction fails part-way the earlier ones are put back, so the stock is either fully
     * updated or left untouched.
     */
    public StageUsageResult recordStageUsage(Order order, String stageName, List<Long> itemIds,
                                             List<String> quantities, String actor) {
        Map<Long, Integer> requested = new LinkedHashMap<>();
        int count = Math.min(itemIds == null ? 0 : itemIds.size(), quantities == null ? 0 : quantities.size());
        for (int i = 0; i < count; i++) {
            String raw = quantities.get(i) == null ? "" : quantities.get(i).trim();
            if (raw.isEmpty()) {
                continue;
            }
            int amount;
            try {
                amount = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                return StageUsageResult.failure("Supplies used must be whole numbers.");
            }
            if (amount < 0) {
                return StageUsageResult.failure("Supplies used cannot be negative.");
            }
            if (amount == 0) {
                continue;
            }
            requested.merge(itemIds.get(i), amount, (a, b) -> (int) Math.min((long) a + b, Integer.MAX_VALUE));
        }

        if (requested.isEmpty()) {
            return StageUsageResult.failure("Enter the supplies used for the " + stageName.toLowerCase()
                    + " stage (at least one item) before advancing this order.");
        }

        Map<InventoryItem, Integer> toUse = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : requested.entrySet()) {
            Optional<InventoryItem> found = inventoryItemRepository.findById(entry.getKey());
            if (found.isEmpty()) {
                return StageUsageResult.failure("One of the selected inventory items no longer exists.");
            }
            InventoryItem item = found.get();
            if (item.getQuantity() < entry.getValue()) {
                return StageUsageResult.failure("Not enough " + item.getItemName() + " in stock (available "
                        + item.getQuantity() + ", entered " + entry.getValue() + ").");
            }
            toUse.put(item, entry.getValue());
        }

        String reason = stageName + " stage, order " + order.getTrackingId();
        Map<InventoryItem, Integer> done = new LinkedHashMap<>();
        for (Map.Entry<InventoryItem, Integer> entry : toUse.entrySet()) {
            if (!consume(entry.getKey(), entry.getValue(), reason, actor)) {
                for (Map.Entry<InventoryItem, Integer> undo : done.entrySet()) {
                    inventoryItemRepository.addStock(undo.getKey().getId(), undo.getValue(),
                            Integer.MAX_VALUE - undo.getValue(), LocalDateTime.now());
                }
                return StageUsageResult.failure("Stock for " + entry.getKey().getItemName()
                        + " changed while saving. Please try again.");
            }
            done.put(entry.getKey(), entry.getValue());
        }

        List<String> summary = new ArrayList<>();
        for (Map.Entry<InventoryItem, Integer> entry : done.entrySet()) {
            summary.add(entry.getKey().getItemName() + " -" + entry.getValue());
        }
        return StageUsageResult.success(summary);
    }
}
