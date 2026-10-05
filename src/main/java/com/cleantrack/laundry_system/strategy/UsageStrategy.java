package com.cleantrack.laundry_system.strategy;

/**
 * Strategy interface (Strategy pattern).
 * Each implementation is one way of working out how many units of an inventory item
 * an order uses. The Context (InventoryItem) delegates to the strategy that matches its
 * usage basis, so no if/else chain is needed and new rules can be added without changing it.
 */
public interface UsageStrategy {

    /** Value stored in the inventory_items.usage_basis column. */
    String getBasis();

    /** True when stock is deducted automatically at the washing stage. */
    boolean isAutomatic();

    /**
     * Units to deduct for an order.
     *
     * @param garments number of garments in the order
     * @param amount   the item's usage amount (N garments, or units per order)
     */
    int unitsFor(int garments, int amount);

    /** Short text for the inventory page, for example "1 per 20 garments". */
    String describe(int amount);
}
