package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class WorkQueueController {

    private final OrderRepository orderRepository;
    private final com.cleantrack.laundry_system.repository.StatusUpdateRepository statusUpdateRepository;
    private final com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository;
    private final com.cleantrack.laundry_system.service.InventoryStockService inventoryStockService;

    @Autowired
    public WorkQueueController(OrderRepository orderRepository, com.cleantrack.laundry_system.repository.StatusUpdateRepository statusUpdateRepository, com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository, com.cleantrack.laundry_system.service.InventoryStockService inventoryStockService) {
        this.orderRepository = orderRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.auditLogRepository = auditLogRepository;
        this.inventoryStockService = inventoryStockService;
    }

    @org.springframework.web.bind.annotation.GetMapping("/queue")
    public String viewQueue(jakarta.servlet.http.HttpSession session, org.springframework.ui.Model model,
                            @org.springframework.web.bind.annotation.RequestParam(value = "stage", required = false) String stage,
                            @org.springframework.web.bind.annotation.RequestParam(value = "search", required = false) String search) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        // Fetch active orders (not completed, not cancelled)
        java.util.List<com.cleantrack.laundry_system.model.Order> allOrders = orderRepository.findAll();
        java.util.List<com.cleantrack.laundry_system.model.Order> activeOrders = new java.util.ArrayList<>();
        
        // Optional stage filter and tracking-ID search (both come from the query string)
        String stageFilter = (stage == null || stage.trim().isEmpty()) ? null : stage.trim();
        String searchTerm = (search == null || search.trim().isEmpty()) ? null : search.trim();

        for (com.cleantrack.laundry_system.model.Order o : allOrders) {
            // "COMPLETED" = removed from the queue; "Order Completed" stays visible until it is deleted manually
            if ("COMPLETED".equals(o.getStatus()) || "Cancelled".equals(o.getStatus())) {
                continue;
            }
            if (stageFilter != null && !stageFilter.equals(o.getStatus())) {
                continue;
            }
            if (searchTerm != null && (o.getTrackingId() == null
                    || !o.getTrackingId().toLowerCase().contains(searchTerm.toLowerCase()))) {
                continue;
            }
            activeOrders.add(o);
        }
        
        // A search term that matches no order at all (any status) is an incorrect Tracking ID
        boolean incorrectTrackingId = false;
        if (searchTerm != null) {
            incorrectTrackingId = true;
            for (com.cleantrack.laundry_system.model.Order o : allOrders) {
                if (o.getTrackingId() != null
                        && o.getTrackingId().toLowerCase().contains(searchTerm.toLowerCase())) {
                    incorrectTrackingId = false;
                    break;
                }
            }
        }

        model.addAttribute("orders", activeOrders);

        // Orders whose next stage is washing/cleaning need the employee to record the supplies used (UC-05, 3a)
        java.util.Set<Long> usageOrderIds = new java.util.HashSet<>();
        for (com.cleantrack.laundry_system.model.Order o : activeOrders) {
            if (isUsageStage(getNextStage(o.getStatus(), o.getServiceType()))) {
                usageOrderIds.add(o.getId());
            }
        }
        model.addAttribute("usageOrderIds", usageOrderIds);
        model.addAttribute("inventoryItems", inventoryStockService.listItems());
        model.addAttribute("selectedStage", stageFilter);
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("incorrectTrackingId", incorrectTrackingId);
        model.addAttribute("user", user);
        return "work-queue";
    }

    @org.springframework.web.bind.annotation.PostMapping("/queue/{id}/advance")
    public String advanceStage(@org.springframework.web.bind.annotation.PathVariable Long id, jakarta.servlet.http.HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
                               @org.springframework.web.bind.annotation.RequestParam(value = "itemId", required = false) java.util.List<Long> itemIds,
                               @org.springframework.web.bind.annotation.RequestParam(value = "usedQty", required = false) java.util.List<String> usedQtys) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        java.util.Optional<com.cleantrack.laundry_system.model.Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/queue?error=OrderNotFound";
        }
        
        com.cleantrack.laundry_system.model.Order order = optOrder.get();
        String currentStatus = order.getStatus();
        String nextStatus = getNextStage(currentStatus, order.getServiceType());

        if (nextStatus == null) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be advanced further.");
            return "redirect:/queue";
        }

        // Only Laundry Staff, Admin, or Branch Supervisor can advance stages. Counter staff can only accept or hand out.
        String role = user.getRole() != null ? user.getRole().name() : "";
        if ("COUNTER_STAFF".equals(role) && !currentStatus.equals("Order Placed") && !currentStatus.equals("READY FOR COLLECTION")) {
             redirectAttributes.addFlashAttribute("error", "You do not have permission to advance laundry processing stages.");
             return "redirect:/queue";
        }

        // UC-05 extension 3a: when the order reaches the washing (or dry-clean cleaning) stage, the employee records
        // the supplies actually used. The stock is deducted before the order advances; a problem stops the advance.
        String successMessage = "Order advanced to " + nextStatus;
        if (isUsageStage(nextStatus) && !inventoryStockService.listItems().isEmpty()) {
            com.cleantrack.laundry_system.service.InventoryStockService.StageUsageResult usage =
                    inventoryStockService.recordStageUsage(order, nextStatus, itemIds, usedQtys, user.getFullName());
            if (!usage.isSuccess()) {
                redirectAttributes.addFlashAttribute("error", usage.getError());
                return "redirect:/queue";
            }
            successMessage += ". Supplies used: " + String.join(", ", usage.getUsed()) + ".";
        }

        order.setStatus(nextStatus);
        orderRepository.save(order);

        // Record history
        com.cleantrack.laundry_system.model.StatusUpdate update = new com.cleantrack.laundry_system.model.StatusUpdate(order, nextStatus, user);
        statusUpdateRepository.save(update);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order ID " + order.getId() + " advanced to " + nextStatus + " by " + user.getFullName()));

        // Logging as MVP Notification
        System.out.println("========== NOTIFICATION ==========");
        System.out.println("To Customer: Order " + order.getTrackingId() + " has reached stage: " + nextStatus);
        System.out.println("==================================");

        redirectAttributes.addFlashAttribute("success", successMessage);
        return "redirect:/queue";
    }

    @org.springframework.web.bind.annotation.PostMapping("/queue/{id}/cancel")
    public String cancelOrder(@org.springframework.web.bind.annotation.PathVariable Long id, jakarta.servlet.http.HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        java.util.Optional<com.cleantrack.laundry_system.model.Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/queue?error=OrderNotFound";
        }
        
        com.cleantrack.laundry_system.model.Order order = optOrder.get();
        String currentStatus = order.getStatus();

        if ("COMPLETED".equals(currentStatus) || "Order Completed".equals(currentStatus) || "Cancelled".equals(currentStatus)) {
            redirectAttributes.addFlashAttribute("error", "This order cannot be cancelled anymore.");
            return "redirect:/queue";
        }

        // Only Branch Supervisors or Admins should be able to cancel mid-processing
        String role = user.getRole() != null ? user.getRole().name() : "";
        if ("LAUNDRY_STAFF".equals(role) || "COUNTER_STAFF".equals(role)) {
             redirectAttributes.addFlashAttribute("error", "Only Supervisors or Admins can cancel an order in processing.");
             return "redirect:/queue";
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);

        // Record history
        com.cleantrack.laundry_system.model.StatusUpdate update = new com.cleantrack.laundry_system.model.StatusUpdate(order, "Cancelled", user);
        statusUpdateRepository.save(update);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order ID " + order.getId() + " cancelled by " + user.getFullName()));

        System.out.println("========== NOTIFICATION ==========");
        System.out.println("To Customer: Order " + order.getTrackingId() + " processing has been Cancelled.");
        System.out.println("==================================");

        redirectAttributes.addFlashAttribute("success", "Order has been cancelled.");
        return "redirect:/queue";
    }
    
    // Manual removal of a finished order from the queue. The order record itself is kept (soft delete):
    // its status becomes "COMPLETED", which the queue view hides, and the removal is logged.
    @org.springframework.web.bind.annotation.PostMapping("/queue/{id}/delete")
    public String deleteCompletedOrder(@org.springframework.web.bind.annotation.PathVariable Long id, jakarta.servlet.http.HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");

        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        java.util.Optional<com.cleantrack.laundry_system.model.Order> optOrder = orderRepository.findById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/queue?error=OrderNotFound";
        }

        com.cleantrack.laundry_system.model.Order order = optOrder.get();
        if (!"Order Completed".equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only completed orders can be deleted from the queue.");
            return "redirect:/queue";
        }

        order.setStatus("COMPLETED");
        orderRepository.save(order);

        // Record history
        com.cleantrack.laundry_system.model.StatusUpdate update = new com.cleantrack.laundry_system.model.StatusUpdate(order, "REMOVED FROM QUEUE", user);
        statusUpdateRepository.save(update);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order ID " + order.getId() + " removed from the queue by " + user.getFullName()));

        redirectAttributes.addFlashAttribute("success", "Order " + order.getTrackingId() + " has been deleted from the queue.");
        return "redirect:/queue";
    }

    @org.springframework.web.bind.annotation.GetMapping("/queue/{id}/history")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.List<java.util.Map<String, String>> getHistory(@org.springframework.web.bind.annotation.PathVariable Long id) {
        java.util.List<com.cleantrack.laundry_system.model.StatusUpdate> updates = statusUpdateRepository.findByOrderIdOrderByChangedAtDesc(id);
        java.util.List<java.util.Map<String, String>> historyList = new java.util.ArrayList<>();
        
        for (com.cleantrack.laundry_system.model.StatusUpdate u : updates) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("stage", u.getStage());
            map.put("updatedBy", u.getUpdatedBy().getFullName());
            map.put("date", u.getChangedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            historyList.add(map);
        }
        return historyList;
    }

    // Stage flow per service type.
    // Common stages: "Order Placed" and "RECEIVED" at the start, "READY FOR COLLECTION" at the end.
    // Advancing from "READY FOR COLLECTION" moves the order to "Order Completed"; it is then removed from the queue manually (Delete button).
    private java.util.List<String> getStagePath(String serviceType) {
        java.util.List<String> path = new java.util.ArrayList<>();
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

    // Stages at which the employee must record the supplies used.
    private boolean isUsageStage(String stage) {
        return "WASHING".equals(stage) || "CLEANING".equals(stage);
    }

    private String getNextStage(String current, String serviceType) {
        if (current == null) return "RECEIVED";

        java.util.List<String> path = getStagePath(serviceType);
        int index = path.indexOf(current);
        if (index < 0 || index >= path.size() - 1) {
            return null;
        }
        return path.get(index + 1);
    }
}
