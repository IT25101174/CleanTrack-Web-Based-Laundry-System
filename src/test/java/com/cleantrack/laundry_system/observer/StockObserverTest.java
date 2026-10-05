package com.cleantrack.laundry_system.observer;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockObserverTest {

    /** A repository that only records the audit entries passed to save(). */
    private static AuditLogRepository recordingRepository(List<String> messages) {
        return (AuditLogRepository) Proxy.newProxyInstance(
                AuditLogRepository.class.getClassLoader(),
                new Class<?>[]{AuditLogRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName()) && args != null && args[0] instanceof AuditLog log) {
                        messages.add(log.getActionPerformed());
                        return log;
                    }
                    return null;
                });
    }

    @Test
    void auditObserverRecordsConsumption() {
        List<String> messages = new ArrayList<>();
        StockObserver observer = new StockAuditObserver(recordingRepository(messages));

        observer.onStockConsumed(new StockChange("Detergent", 100, 3, 20, "manual", "supervisor1"));

        assertEquals(List.of("Consumed 3 units of Detergent (manual) by supervisor1"), messages);
    }

    @Test
    void auditObserverRecordsSkippedDeduction() {
        List<String> messages = new ArrayList<>();
        StockObserver observer = new StockAuditObserver(recordingRepository(messages));

        observer.onDeductionSkipped("Softener", "CT-1");

        assertEquals(List.of("Automatic stock deduction skipped for order CT-1: not enough Softener"), messages);
    }

    @Test
    void lowStockObserverAlertsOnlyWhenThresholdIsCrossed() {
        List<String> messages = new ArrayList<>();
        StockObserver observer = new LowStockAlertObserver(recordingRepository(messages));

        observer.onStockConsumed(new StockChange("Detergent", 30, 6, 25, "manual", "sup"));
        assertEquals(List.of("LOW STOCK ALERT: Detergent fell to 24 (threshold 25)"), messages);

        observer.onStockConsumed(new StockChange("Detergent", 24, 1, 25, "manual", "sup"));
        assertEquals(1, messages.size());
    }

    @Test
    void stockChangeKnowsWhenItCrossedTheThreshold() {
        assertTrue(new StockChange("A", 25, 1, 25, "r", "u").crossedBelowThreshold());
        assertFalse(new StockChange("A", 30, 5, 25, "r", "u").crossedBelowThreshold());
        assertFalse(new StockChange("A", 20, 1, 25, "r", "u").crossedBelowThreshold());
    }
}
