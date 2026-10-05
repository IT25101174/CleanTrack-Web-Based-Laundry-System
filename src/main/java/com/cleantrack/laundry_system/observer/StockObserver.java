package com.cleantrack.laundry_system.observer;

/**
 * Observer interface (Observer pattern).
 * The subject (InventoryStockService) calls these methods whenever stock is deducted.
 * Each observer reacts in its own way, so new reactions (for example an e-mail alert)
 * can be added as new classes without touching the service.
 */
public interface StockObserver {

    /** Called after stock has been deducted successfully. */
    void onStockConsumed(StockChange change);

    /** Called when an automatic deduction was skipped because there was not enough stock. */
    default void onDeductionSkipped(String itemName, String orderReference) {
        // most observers do not need this event
    }
}
