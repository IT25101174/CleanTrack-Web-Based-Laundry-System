package com.cleantrack.laundry_system.observer;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Concrete observer: writes one LOW STOCK ALERT when a deduction takes an item from at or above
 * its threshold to below it, so the alert is not repeated for every later deduction.
 */
@Component
@Order(2)
public class LowStockAlertObserver implements StockObserver {

    private final AuditLogRepository auditLogRepository;

    public LowStockAlertObserver(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void onStockConsumed(StockChange change) {
        if (change.crossedBelowThreshold()) {
            auditLogRepository.save(new AuditLog("LOW STOCK ALERT: " + change.getItemName() + " fell to "
                    + change.getQuantityAfter() + " (threshold " + change.getLowStockThreshold() + ")"));
        }
    }
}
