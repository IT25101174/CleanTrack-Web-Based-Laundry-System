package com.cleantrack.laundry_system.strategy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Chooses the strategy for a usage basis. The strategies hold no state, so one shared
 * instance of each is reused. An unknown or missing basis falls back to manual use.
 */
public final class UsageStrategyFactory {

    private static final Map<String, UsageStrategy> STRATEGIES = new LinkedHashMap<>();

    static {
        register(new ManualUsageStrategy());
        register(new PerGarmentsUsageStrategy());
        register(new PerOrderUsageStrategy());
    }

    private UsageStrategyFactory() {
    }

    private static void register(UsageStrategy strategy) {
        STRATEGIES.put(strategy.getBasis(), strategy);
    }

    /** Returns the strategy for the basis, or the manual strategy when it is null or unknown. */
    public static UsageStrategy forBasis(String basis) {
        if (basis == null) {
            return STRATEGIES.get(ManualUsageStrategy.BASIS);
        }
        UsageStrategy strategy = STRATEGIES.get(basis.trim());
        return strategy != null ? strategy : STRATEGIES.get(ManualUsageStrategy.BASIS);
    }

    /** True when the basis names a known strategy. */
    public static boolean isKnown(String basis) {
        return basis != null && STRATEGIES.containsKey(basis.trim());
    }
}
