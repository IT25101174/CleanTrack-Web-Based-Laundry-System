package com.cleantrack.laundry_system.strategy;

/** Concrete strategy: a fixed number of units for every order, whatever its size (for example softener). */
public class PerOrderUsageStrategy implements UsageStrategy {

    public static final String BASIS = "PER_ORDER";

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
        return amount;
    }

    @Override
    public String describe(int amount) {
        return amount + " per order";
    }
}
