package com.cleantrack.laundry_system.observer;

/** Immutable description of one stock deduction, sent by the subject to every observer. */
public class StockChange {

    private final String itemName;
    private final int quantityBefore;
    private final int quantityAfter;
    private final int lowStockThreshold;
    private final int amount;
    private final String reason;
    private final String actor;

    public StockChange(String itemName, int quantityBefore, int amount, int lowStockThreshold,
                       String reason, String actor) {
        this.itemName = itemName;
        this.quantityBefore = quantityBefore;
        this.amount = amount;
        this.quantityAfter = quantityBefore - amount;
        this.lowStockThreshold = lowStockThreshold;
        this.reason = reason;
        this.actor = actor;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public int getLowStockThreshold() {
        return lowStockThreshold;
    }

    public int getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getActor() {
        return actor;
    }

    /** True when this deduction took the item from at or above its threshold to below it. */
    public boolean crossedBelowThreshold() {
        return quantityBefore >= lowStockThreshold && quantityAfter < lowStockThreshold;
    }
}
