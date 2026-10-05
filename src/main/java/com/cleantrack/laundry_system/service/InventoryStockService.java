package com.cleantrack.laundry_system.service;

import com.cleantrack.laundry_system.model.InventoryItem;
import com.cleantrack.laundry_system.model.Order;
import com.cleantrack.laundry_system.observer.StockChange;
import com.cleantrack.laundry_system.observer.StockObserver;
import com.cleantrack.laundry_system.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central place for reducing stock. It is used by the manual "Use" action in the inventory page
 * and by the work queue, which deducts supplies automatically when an order reaches the washing
 * stage (UC-05, extension 3a).
 *
 * Design patterns:
 * - Observer: this class is the subject. After each deduction it notifies every registered
 *   StockObserver (audit log, low-stock alert, ...). Spring injects all StockObserver beans.
 * - Strategy: how many units an order uses is decided by the item's UsageStrategy
 *   (see InventoryItem.unitsFor).
 */
@Service
public class InventoryStockService {

    private final InventoryItemRepository inventoryItemRepository;
    private final List<StockObserver> observers = new CopyOnWriteArrayList<>();

    @Autowired
    public InventoryStockService(InventoryItemRepository inventoryItemRepository,
                                 List<StockObserver> stockObservers) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.observers.addAll(stockObservers);
    }

    /** Adds an observer at run time. */
    public void registerObserver(StockObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /** Removes an observer. */
    public void removeObserver(StockObserver observer) {
        observers.remove(observer);
    }

    private void notifyConsumed(StockChange change) {
        for (StockObserver observer : observers) {
            observer.onStockConsumed(change);
        }
    }

    private void notifySkipped(String itemName, String orderReference) {
        for (StockObserver observer : observers) {
            observer.onDeductionSkipped(itemName, orderReference);
        }
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
     * On success every observer is notified (audit entry, low-stock alert, ...).
     */
    public boolean consume(InventoryItem item, int amount, String reason, String actor) {
        int quantityBefore = item.getQuantity();
        int updated = inventoryItemRepository.removeStock(item.getId(), amount, LocalDateTime.now());
        if (updated == 0) {
            return false;
        }

        notifyConsumed(new StockChange(item.getItemName(), quantityBefore, amount,
                item.getLowStockThreshold(), reason, actor));
        return true;
    }

    /**
     * Automatically deducts the supplies for an order that has just reached a processing stage
     * (washing, or cleaning for dry clean). Only items that have a usage rule applying to the order's
     * service type are deducted. The amount comes from the item's usage strategy: either 1 unit per N
     * garments (rounded up) or a fixed number of units per order. A shortage never blocks the order:
     * that item is left unchanged and reported in the result and to the observers.
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
                notifySkipped(item.getItemName(), order.getTrackingId());
            }
        }
        return result;
    }
}
