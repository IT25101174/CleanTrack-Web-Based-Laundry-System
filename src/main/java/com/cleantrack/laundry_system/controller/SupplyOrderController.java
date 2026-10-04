package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.model.InventoryItem;
import com.cleantrack.laundry_system.model.Supplier;
import com.cleantrack.laundry_system.model.SupplyOrder;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import com.cleantrack.laundry_system.repository.InventoryItemRepository;
import com.cleantrack.laundry_system.repository.SupplierRepository;
import com.cleantrack.laundry_system.repository.SupplyOrderRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/supply-orders")
public class SupplyOrderController {

    private static final int MAX_ORDER_QUANTITY = 1_000_000;

    private final SupplyOrderRepository supplyOrderRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final SupplierRepository supplierRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public SupplyOrderController(SupplyOrderRepository supplyOrderRepository,
                                 InventoryItemRepository inventoryItemRepository,
                                 SupplierRepository supplierRepository,
                                 AuditLogRepository auditLogRepository) {
        this.supplyOrderRepository = supplyOrderRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.supplierRepository = supplierRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listOrders(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }

        List<InventoryItem> items = inventoryItemRepository.findAll().stream()
                .sorted(Comparator.comparing(InventoryItem::getItemName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        model.addAttribute("orders", supplyOrderRepository.findAllByOrderByOrderedAtDescIdDesc());
        model.addAttribute("items", items);
        model.addAttribute("suppliers", supplierRepository.findAllByOrderBySupplierNameAsc());
        model.addAttribute("pendingCount", supplyOrderRepository.countByStatus(SupplyOrder.PENDING));
        model.addAttribute("receivedCount", supplyOrderRepository.countByStatus(SupplyOrder.RECEIVED));
        model.addAttribute("user", user);
        model.addAttribute("canManage", canManage(user));
        return "supply-order-list";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam Long itemId,
                              @RequestParam Long supplierId,
                              @RequestParam String quantity,
                              @RequestParam(required = false) String notes,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage supply orders.");
            return "redirect:/supply-orders";
        }

        Integer parsedQuantity = parsePositiveInt(quantity);
        if (parsedQuantity == null) {
            redirectAttributes.addFlashAttribute("error", "Quantity must be a whole number greater than zero.");
            return "redirect:/supply-orders";
        }
        if (parsedQuantity > MAX_ORDER_QUANTITY) {
            redirectAttributes.addFlashAttribute("error", "Quantity cannot exceed " + MAX_ORDER_QUANTITY + " per order.");
            return "redirect:/supply-orders";
        }

        String cleanNotes = clean(notes);
        if (cleanNotes != null && cleanNotes.length() > 200) {
            redirectAttributes.addFlashAttribute("error", "Notes cannot exceed 200 characters.");
            return "redirect:/supply-orders";
        }

        Optional<InventoryItem> item = inventoryItemRepository.findById(itemId);
        if (item.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Inventory item not found.");
            return "redirect:/supply-orders";
        }
        Optional<Supplier> supplier = supplierRepository.findById(supplierId);
        if (supplier.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Supplier not found.");
            return "redirect:/supply-orders";
        }

        SupplyOrder order = new SupplyOrder(
                item.get().getId(), item.get().getItemName(),
                supplier.get().getId(), supplier.get().getSupplierName(),
                parsedQuantity, truncate(user.getFullName(), 100), cleanNotes);
        supplyOrderRepository.save(order);
        auditLogRepository.save(new AuditLog("Supply order created: " + parsedQuantity + " x "
                + order.getItemName() + " from " + order.getSupplierName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Supply order created.");
        return "redirect:/supply-orders";
    }

    // Receiving an order adds its quantity to the inventory item. The item and the
    // order are saved together, so either both change or neither does.
    @PostMapping("/receive/{id}")
    @Transactional
    public String receiveOrder(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage supply orders.");
            return "redirect:/supply-orders";
        }

        Optional<SupplyOrder> found = supplyOrderRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Supply order not found.");
            return "redirect:/supply-orders";
        }
        SupplyOrder order = found.get();
        if (!SupplyOrder.PENDING.equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only pending supply orders can be received.");
            return "redirect:/supply-orders";
        }

        Optional<InventoryItem> itemOpt = inventoryItemRepository.findById(order.getItemId());
        if (itemOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "The inventory item for this order no longer exists. Cancel the order instead.");
            return "redirect:/supply-orders";
        }
        InventoryItem item = itemOpt.get();

        long newQuantity = item.getQuantity().longValue() + order.getQuantity().longValue();
        if (newQuantity > Integer.MAX_VALUE) {
            redirectAttributes.addFlashAttribute("error", "Receiving this order would exceed the maximum stock quantity.");
            return "redirect:/supply-orders";
        }

        LocalDateTime now = LocalDateTime.now();
        item.setQuantity((int) newQuantity);
        item.setUpdatedAt(now);
        inventoryItemRepository.save(item);

        order.setStatus(SupplyOrder.RECEIVED);
        order.setReceivedAt(now);
        supplyOrderRepository.save(order);

        auditLogRepository.save(new AuditLog("Supply order #" + order.getId() + " received: +" + order.getQuantity()
                + " units of " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Supply order received. " + item.getItemName()
                + " stock is now " + item.getQuantity() + ".");
        return "redirect:/supply-orders";
    }

    @PostMapping("/cancel/{id}")
    public String cancelOrder(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage supply orders.");
            return "redirect:/supply-orders";
        }

        Optional<SupplyOrder> found = supplyOrderRepository.findById(id);
        if (found.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Supply order not found.");
            return "redirect:/supply-orders";
        }
        SupplyOrder order = found.get();
        if (!SupplyOrder.PENDING.equals(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Only pending supply orders can be cancelled.");
            return "redirect:/supply-orders";
        }

        order.setStatus(SupplyOrder.CANCELLED);
        supplyOrderRepository.save(order);
        auditLogRepository.save(new AuditLog("Supply order #" + order.getId() + " cancelled by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Supply order cancelled.");
        return "redirect:/supply-orders";
    }

    private boolean isCustomer(User user) {
        return user.getRole() != null && "CUSTOMER".equals(user.getRole().name());
    }

    private boolean canManage(User user) {
        return user.getRole() != null && "BRANCH_SUPERVISOR".equals(user.getRole().name());
    }

    private Integer parsePositiveInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}