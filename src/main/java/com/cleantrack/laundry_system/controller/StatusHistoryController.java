package com.cleantrack.controller;

import com.cleantrack.model.AuditLog;
import com.cleantrack.model.Order;
import com.cleantrack.model.StatusUpdate;
import com.cleantrack.model.User;
import com.cleantrack.repository.AuditLogRepository;
import com.cleantrack.repository.OrderRepository;
import com.cleantrack.repository.StatusUpdateRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Status History page.
 *
 *  - Create : history records are created automatically (WorkQueueController and the revert below).
 *  - Read   : every staff role can view the complete timeline of any order, found by Tracking ID.
 *             Orders removed from the queue ("COMPLETED") and cancelled orders are included,
 *             because nothing is ever physically deleted.
 *  - Update : "Revert stage" - Admin and Branch Supervisor only. Moves the order back one stage,
 *             adds a new history record and marks the mistaken one as reverted.
 *  - Delete : none. History records are never deleted.
 */
@Controller
public class StatusHistoryController {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final OrderRepository orderRepository;
    private final StatusUpdateRepository statusUpdateRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public StatusHistoryController(OrderRepository orderRepository,
                                   StatusUpdateRepository statusUpdateRepository,
                                   AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ---------------------------------------------------------------- READ

    @GetMapping("/status-history")
    public String viewStatusHistory(HttpSession session, Model model,
                                    @RequestParam(value = "search", required = false) String search) {
        User user = (User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(roleName(user))) {
            return "redirect:/login";
        }

        String searchTerm = (search == null || search.trim().isEmpty()) ? null : search.trim();

        // All orders, whatever their status (removed and cancelled orders included), newest first
        List<Order> allOrders = new ArrayList<>(orderRepository.findAll());
        allOrders.sort(Comparator.comparing(Order::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        Order selected = null;
        if (searchTerm != null) {
            for (Order o : allOrders) {
                if (o.getTrackingId() != null && o.getTrackingId().equalsIgnoreCase(searchTerm)) {
                    selected = o;
                    break;
                }
            }
        }

        if (selected != null) {
            List<HistoryRow> rows = new ArrayList<>();
            for (StatusUpdate u : statusUpdateRepository.findByOrderIdOrderByChangedAtAscIdAsc(selected.getId())) {
                rows.add(new HistoryRow(u));
            }
            model.addAttribute("history", rows);

            String blockedReason = getRevertBlockedReason(selected);
            model.addAttribute("revertTarget", blockedReason == null ? getPreviousStage(selected) : null);
            model.addAttribute("revertBlockedReason", blockedReason);
        }

        String role = roleName(user);
        model.addAttribute("canRevert", "ADMIN".equals(role) || "BRANCH_SUPERVISOR".equals(role));
        model.addAttribute("selectedOrder", selected);
        model.addAttribute("incorrectTrackingId", searchTerm != null && selected == null);
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("allOrders", allOrders);
        model.addAttribute("user", user);
        return "status-history";
    }

    // -------------------------------------------------------------- UPDATE

    @PostMapping("/status-history/{id}/revert")
    @Transactional
    public String revertStage(@PathVariable Long id,
                              @RequestParam(value = "reason", required = false) String reason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(roleName(user))) {
            return "redirect:/login";
        }

        Optional<Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/status-history";
        }
        Order order = optOrder.get();

        // Only Admin and Branch Supervisor may revert
        String role = roleName(user);
        if (!"ADMIN".equals(role) && !"BRANCH_SUPERVISOR".equals(role)) {
            redirectAttributes.addFlashAttribute("error", "Only Admins or Branch Supervisors can revert an order's stage.");
            return backToOrder(order, redirectAttributes);
        }

        // A reason is required
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a reason for reverting the stage.");
            return backToOrder(order, redirectAttributes);
        }
        if (cleanReason.length() > 200) {
            redirectAttributes.addFlashAttribute("error", "The reason must be 200 characters or fewer.");
            return backToOrder(order, redirectAttributes);
        }

        // Cancelled / removed orders and the first stage cannot be reverted
        String blockedReason = getRevertBlockedReason(order);
        if (blockedReason != null) {
            redirectAttributes.addFlashAttribute("error", blockedReason);
            return backToOrder(order, redirectAttributes);
        }

        String currentStage = order.getStatus();
        String previousStage = getPreviousStage(order);

        // Mark the record that the mistaken advance created (latest, not-yet-reverted record for the current stage)
        List<StatusUpdate> records = statusUpdateRepository.findByOrderIdOrderByChangedAtAscIdAsc(order.getId());
        for (int i = records.size() - 1; i >= 0; i--) {
            StatusUpdate record = records.get(i);
            if (!record.isReverted() && currentStage.equals(record.getStage())) {
                record.setReverted(true);
                record.setRevertReason(cleanReason);
                record.setRevertedBy(user);
                record.setRevertedAt(LocalDateTime.now());
                statusUpdateRepository.save(record);
                break;
            }
        }

        // Move the order back one stage and add a new history record (nothing is erased)
        order.setStatus(previousStage);
        orderRepository.save(order);

        StatusUpdate newRecord = new StatusUpdate(order, previousStage, user);
        newRecord.setNote("Reverted from " + currentStage + ". Reason: " + cleanReason);
        statusUpdateRepository.save(newRecord);

        auditLogRepository.save(new AuditLog("Order ID " + order.getId() + " reverted from " + currentStage
                + " to " + previousStage + " by " + user.getFullName() + ". Reason: " + cleanReason));

        redirectAttributes.addFlashAttribute("success", "Order reverted to " + previousStage);
        return backToOrder(order, redirectAttributes);
    }

    // ------------------------------------------------------------- helpers

    private String backToOrder(Order order, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("search", order.getTrackingId());
        return "redirect:/status-history";
    }

    private String roleName(User user) {
        return user.getRole() != null ? user.getRole().name() : "";
    }

    // Same stage flow per service type as WorkQueueController (kept here so that controller stays untouched).
    // Common stages: "Order Placed", "RECEIVED" at the start; "READY FOR COLLECTION", "Order Completed" at the end.
    private List<String> getStagePath(String serviceType) {
        List<String> path = new ArrayList<>();
        path.add("Order Placed");
        path.add("RECEIVED");

        String type = serviceType == null ? "" : serviceType.trim().toUpperCase();
        switch (type) {
            case "DRY_CLEAN":
                path.add("GARMENT INSPECTION");
                path.add("CLEANING");
                path.add("DRYING");
                path.add("IRONING");
                break;
            case "WASH_IRON":
                path.add("WASHING");
                path.add("DRYING");
                path.add("IRONING");
                break;
            case "WASH_ONLY":
            default: // no selection (or unrecognised value) defaults to Wash Only
                path.add("WASHING");
                path.add("DRYING");
                break;
        }

        path.add("READY FOR COLLECTION");
        path.add("Order Completed");
        return path;
    }

    // The stage before the order's current one on its own path, or null if there is none
    private String getPreviousStage(Order order) {
        List<String> path = getStagePath(order.getServiceType());
        int index = path.indexOf(order.getStatus());
        return index > 0 ? path.get(index - 1) : null;
    }

    // Returns why the order cannot be reverted, or null if it can
    private String getRevertBlockedReason(Order order) {
        String status = order.getStatus();
        if ("Cancelled".equals(status)) {
            return "Cancelled orders cannot be reverted.";
        }
        if ("COMPLETED".equals(status)) {
            return "Orders already removed from the queue cannot be reverted.";
        }
        if ("Order Placed".equals(status)) {
            return "This order is at its first stage and cannot go back any further.";
        }
        if (getPreviousStage(order) == null) {
            return "This order's current stage is not part of its workflow, so it cannot be reverted.";
        }
        return null;
    }

    /** Read-only view of one history record, with dates already formatted for the page. */
    public static class HistoryRow {
        private final String stage;
        private final String updatedBy;
        private final String date;
        private final String note;
        private final boolean reverted;
        private final String revertReason;
        private final String revertedBy;
        private final String revertedAt;

        HistoryRow(StatusUpdate u) {
            this.stage = u.getStage();
            this.updatedBy = u.getUpdatedBy() != null ? u.getUpdatedBy().getFullName() : "";
            this.date = u.getChangedAt() != null ? u.getChangedAt().format(DATE_FORMAT) : "";
            this.note = u.getNote();
            this.reverted = u.isReverted();
            this.revertReason = u.getRevertReason();
            this.revertedBy = u.getRevertedBy() != null ? u.getRevertedBy().getFullName() : "";
            this.revertedAt = u.getRevertedAt() != null ? u.getRevertedAt().format(DATE_FORMAT) : "";
        }

        public String getStage() { return stage; }
        public String getUpdatedBy() { return updatedBy; }
        public String getDate() { return date; }
        public String getNote() { return note; }
        public boolean isReverted() { return reverted; }
        public String getRevertReason() { return revertReason; }
        public String getRevertedBy() { return revertedBy; }
        public String getRevertedAt() { return revertedAt; }
    }
}
