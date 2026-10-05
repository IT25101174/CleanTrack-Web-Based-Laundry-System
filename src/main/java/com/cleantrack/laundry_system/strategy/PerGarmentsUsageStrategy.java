package com.cleantrack.laundry_system.strategy;

/** Concrete strategy: 1 unit for every N garments in the order, rounded up (for example detergent). */
public class PerGarmentsUsageStrategy implements UsageStrategy {

    public static final String BASIS = "PER_GARMENTS";

    @Override
    public String getBasis() {
        return BASIS;
    }

    @Override
    public boolean isAutomatic() {
        return true;
    }

    @Override
    public int unitsFor(int garments, int amount) {
        if (garments <= 0 || amount <= 0) {
            return 0;
        }
        return (int) Math.ceil(garments / (double) amount);
    }

    @Override
    public String describe(int amount) {
        return "1 per " + amount + " garments";
    }
}
