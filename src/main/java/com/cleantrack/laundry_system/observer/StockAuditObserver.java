package com.cleantrack.laundry_system.observer;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Concrete observer: records every stock deduction (and every skipped one) in the audit log. */
@Component
@Order(1)
public class StockAuditObserver implements StockObserver {

    private final AuditLogRepository auditLogRepository;

    public StockAuditObserver(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void onStockConsumed(StockChange change) {
        auditLogRepository.save(new AuditLog("Consumed " + change.getAmount() + " units of " + change.getItemName()
                + " (" + change.getReason() + ") by " + change.getActor()));
    }

    @Override
    public void onDeductionSkipped(String itemName, String orderReference) {
        auditLogRepository.save(new AuditLog("Automatic stock deduction skipped for order "
                + orderReference + ": not enough " + itemName));
    }
}
