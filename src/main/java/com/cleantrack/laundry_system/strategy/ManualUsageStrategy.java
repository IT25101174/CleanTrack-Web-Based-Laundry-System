package com.cleantrack.laundry_system.strategy;

/** Concrete strategy: the item is never deducted automatically; staff use it by hand. */
public class ManualUsageStrategy implements UsageStrategy {

    public static final String BASIS = "MANUAL";

    @Override
    public String getBasis() {
        return BASIS;
    }

    @Override
    public boolean isAutomatic() {
        return false;
    }

    @Override
    public int unitsFor(int garments, int amount) {
        return 0;
    }

    @Override
    public String describe(int amount) {
        return "Manual only";
    }
}
