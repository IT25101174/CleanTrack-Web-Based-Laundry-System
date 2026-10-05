package com.cleantrack.laundry_system.strategy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsageStrategyTest {

    @Test
    void perGarmentsRoundsUp() {
        UsageStrategy strategy = UsageStrategyFactory.forBasis("PER_GARMENTS");
        assertEquals(3, strategy.unitsFor(45, 20));
        assertEquals(2, strategy.unitsFor(40, 20));
        assertEquals(3, strategy.unitsFor(41, 20));
        assertEquals(1, strategy.unitsFor(1, 20));
    }

    @Test
    void perOrderIgnoresOrderSize() {
        UsageStrategy strategy = UsageStrategyFactory.forBasis("PER_ORDER");
        assertEquals(2, strategy.unitsFor(1, 2));
        assertEquals(2, strategy.unitsFor(500, 2));
    }

    @Test
    void manualNeverDeducts() {
        UsageStrategy strategy = UsageStrategyFactory.forBasis("MANUAL");
        assertFalse(strategy.isAutomatic());
        assertEquals(0, strategy.unitsFor(45, 2));
    }

    @Test
    void invalidInputGivesZeroUnits() {
        assertEquals(0, UsageStrategyFactory.forBasis("PER_GARMENTS").unitsFor(0, 20));
        assertEquals(0, UsageStrategyFactory.forBasis("PER_GARMENTS").unitsFor(45, 0));
        assertEquals(0, UsageStrategyFactory.forBasis("PER_ORDER").unitsFor(45, 0));
    }

    @Test
    void factoryFallsBackToManual() {
        assertEquals("MANUAL", UsageStrategyFactory.forBasis(null).getBasis());
        assertEquals("MANUAL", UsageStrategyFactory.forBasis("UNKNOWN").getBasis());
        assertTrue(UsageStrategyFactory.isKnown("PER_ORDER"));
        assertFalse(UsageStrategyFactory.isKnown("UNKNOWN"));
        assertFalse(UsageStrategyFactory.isKnown(null));
    }

    @Test
    void describesTheRule() {
        assertEquals("1 per 20 garments", UsageStrategyFactory.forBasis("PER_GARMENTS").describe(20));
        assertEquals("2 per order", UsageStrategyFactory.forBasis("PER_ORDER").describe(2));
    }
}
