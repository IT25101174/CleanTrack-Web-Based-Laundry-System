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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private static final String MANAGE_DENIED_MESSAGE = "Only the Branch Supervisor can manage inventory.";

    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final SupplierRepository supplierRepository;
    private final SupplyOrderRepository supplyOrderRepository;

    @Autowired
    public InventoryController(InventoryItemRepository inventoryItemRepository,
                               AuditLogRepository auditLogRepository,
                               SupplierRepository supplierRepository,
                               SupplyOrderRepository supplyOrderRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.supplierRepository = supplierRepository;
        this.supplyOrderRepository = supplyOrderRepository;
    }

    @GetMapping
    public String listInventory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }

        List<InventoryItem> items = inventoryItemRepository.findAll();

        BigDecimal totalValue = BigDecimal.ZERO;
        for (InventoryItem item : items) {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                totalValue = totalValue.add(item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));
            }
        }

        List<Supplier> suppliers = supplierRepository.findAllByOrderBySupplierNameAsc();

        model.addAttribute("items", items);
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierNames", suppliers.stream().map(Supplier::getSupplierName).toList());
        model.addAttribute("canManage", canManage(user));
        model.addAttribute("user", user);
        return "inventory-list";
    }

    @PostMapping("/add")
    public String addItem(@RequestParam String itemName,
                          @RequestParam String quantity,
                          @RequestParam String lowStockThreshold,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String unit,
                          @RequestParam(required = false) String supplier,
                          @RequestParam(required = false) String unitPrice,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", MANAGE_DENIED_MESSAGE);
            return "redirect:/inventory";
        }

        String name = clean(itemName);
        String categoryValue = clean(category);
        String unitValue = clean(unit);
        String supplierValue = clean(supplier);

        String validationError = validateName(name);
        if (validationError == null && parseNonNegativeInt(quantity) == null) {
            validationError = "Quantity must be a whole number of zero or greater.";
        }
        if (validationError == null && parseNonNegativeInt(lowStockThreshold) == null) {
            validationError = "Low stock threshold must be a whole number of zero or greater.";
        }
        if (validationError == null) {
            validationError = validateTextLengths(categoryValue, unitValue, supplierValue);
        }
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/inventory";
        }

        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        if (unitPrice != null && !unitPrice.isBlank() && parsedPrice == null) {
            redirectAttributes.addFlashAttribute("error", "Unit price must be a valid non-negative number.");
            return "redirect:/inventory";
        }

        // The supplier must be one registered under Suppliers (a new item has no earlier value to keep).
        String resolvedSupplier = null;
        if (supplierValue != null) {
            Optional<Supplier> found = supplierRepository.findBySupplierNameIgnoreCase(supplierValue);
            if (found.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Supplier \"" + supplierValue + "\" does not exist. Add it under Suppliers first.");
                return "redirect:/inventory";
            }
            resolvedSupplier = found.get().getSupplierName();
        }

        InventoryItem item = new InventoryItem(
                name,
                parseNonNegativeInt(quantity),
                parseNonNegativeInt(lowStockThreshold),
                categoryValue, unitValue, resolvedSupplier, parsedPrice);
        inventoryItemRepository.save(item);
        auditLogRepository.save(new AuditLog("Inventory item added: " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Item added successfully.");
        return "redirect:/inventory";
    }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id,
                           @RequestParam String itemName,
                           @RequestParam String lowStockThreshold,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String unit,
                           @RequestParam(required = false) String supplier,
                           @RequestParam(required = false) String unitPrice,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", MANAGE_DENIED_MESSAGE);
            return "redirect:/inventory";
        }

        Optional<InventoryItem> existing = inventoryItemRepository.findById(id);
        if (existing.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/inventory";
        }
        InventoryItem item = existing.get();

        String name = clean(itemName);
        String categoryValue = clean(category);
        String unitValue = clean(unit);
        String supplierValue = clean(supplier);

        String validationError = validateName(name);
        Integer parsedThreshold = parseNonNegativeInt(lowStockThreshold);
        if (validationError == null && parsedThreshold == null) {
            validationError = "Low stock threshold must be a whole number of zero or greater.";
        }
        if (validationError == null) {
            validationError = validateTextLengths(categoryValue, unitValue, supplierValue);
        }
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/inventory";
        }

        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        if (unitPrice != null && !unitPrice.isBlank() && parsedPrice == null) {
            redirectAttributes.addFlashAttribute("error", "Unit price must be a valid non-negative number.");
            return "redirect:/inventory";
        }

        // A changed supplier must be a registered one. An unchanged value is kept as it is,
        // so items created before supplier management existed can still be edited.
        String resolvedSupplier = supplierValue;
        if (supplierValue != null && !supplierValue.equalsIgnoreCase(item.getSupplier())) {
            Optional<Supplier> found = supplierRepository.findBySupplierNameIgnoreCase(supplierValue);
            if (found.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Supplier \"" + supplierValue + "\" does not exist. Add it under Suppliers first.");
                return "redirect:/inventory";
            }
            resolvedSupplier = found.get().getSupplierName();
        } else if (supplierValue != null) {
            resolvedSupplier = item.getSupplier();
        }

        item.setItemName(name);
        item.setLowStockThreshold(parsedThreshold);
        item.setCategory(categoryValue);
        item.setUnit(unitValue);
        item.setSupplier(resolvedSupplier);
        item.setUnitPrice(parsedPrice);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);
        auditLogRepository.save(new AuditLog("Updated inventory details for " + name + " by " + user.getFullName()));

        redirectAttributes.addFlashAttribute("success", "Inventory item updated successfully.");
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", MANAGE_DENIED_MESSAGE);
            return "redirect:/inventory";
        }

        Optional<InventoryItem> existing = inventoryItemRepository.findById(id);
        if (existing.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/inventory";
        }
        InventoryItem item = existing.get();

        // An item with a pending supply order cannot be received later, so block the delete.
        if (supplyOrderRepository.existsByItemIdAndStatus(item.getId(), SupplyOrder.PENDING)) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot delete " + item.getItemName()
                            + " because it has pending supply orders. Receive or cancel them first.");
            return "redirect:/inventory";
        }

        inventoryItemRepository.delete(item);
        auditLogRepository.save(new AuditLog("Deleted inventory item " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Inventory item removed.");
        return "redirect:/inventory";
    }

    @PostMapping("/restock/{id}")
    public String restockItem(@PathVariable Long id,
                              @RequestParam String amount,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", MANAGE_DENIED_MESSAGE);
            return "redirect:/inventory";
        }

        Integer parsedAmount = parseNonNegativeInt(amount);
        if (parsedAmount == null || parsedAmount == 0) {
            redirectAttributes.addFlashAttribute("error", "Restock amount must be a whole number greater than zero.");
            return "redirect:/inventory";
        }

        Optional<InventoryItem> existing = inventoryItemRepository.findById(id);
        if (existing.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/inventory";
        }
        InventoryItem item = existing.get();

        // Atomic update in the database: it adds the amount only if the result still fits in an int,
        // so the quantity can never overflow and two simultaneous restocks cannot overwrite each other.
        int updated = inventoryItemRepository.addStock(
                id, parsedAmount, Integer.MAX_VALUE - parsedAmount, LocalDateTime.now());
        if (updated == 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Restocking " + parsedAmount + " units would exceed the maximum stock quantity for "
                            + item.getItemName() + ".");
            return "redirect:/inventory";
        }

        auditLogRepository.save(new AuditLog("Restocked " + parsedAmount + " units of " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Successfully restocked item.");
        return "redirect:/inventory";
    }

    @PostMapping("/consume/{id}")
    public String consumeItem(@PathVariable Long id,
                              @RequestParam String amount,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }

        Integer parsedAmount = parseNonNegativeInt(amount);
        if (parsedAmount == null || parsedAmount == 0) {
            redirectAttributes.addFlashAttribute("error", "Consume amount must be a whole number greater than zero.");
            return "redirect:/inventory";
        }

        Optional<InventoryItem> existing = inventoryItemRepository.findById(id);
        if (existing.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Item not found.");
            return "redirect:/inventory";
        }
        InventoryItem item = existing.get();

        // Atomic update in the database: stock is reduced only while enough remains,
        // so two simultaneous requests can never take the quantity below zero.
        int updated = inventoryItemRepository.removeStock(id, parsedAmount, LocalDateTime.now());
        if (updated == 0) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot consume more than available stock for " + item.getItemName());
            return "redirect:/inventory";
        }

        auditLogRepository.save(new AuditLog("Consumed " + parsedAmount + " units of " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Successfully recorded usage of " + item.getItemName());
        return "redirect:/inventory";
    }

    private boolean isCustomer(User user) {
        return user.getRole() != null && "CUSTOMER".equals(user.getRole().name());
    }

    // Only the Branch Supervisor (the primary actor of Manage Inventory) may change inventory records.
    private boolean canManage(User user) {
        return user.getRole() != null && "BRANCH_SUPERVISOR".equals(user.getRole().name());
    }

    // Trims the value and converts blank input to null.
    private String clean(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateName(String name) {
        if (name == null) {
            return "Item name cannot be empty.";
        }
        if (name.length() > 100) {
            return "Item name cannot exceed 100 characters.";
        }
        return null;
    }

    // Limits match the column lengths in InventoryItem (category = 50, unit = 30, supplier = 100).
    private String validateTextLengths(String category, String unit, String supplier) {
        if (category != null && category.length() > 50) {
            return "Category cannot exceed 50 characters.";
        }
        if (unit != null && unit.length() > 30) {
            return "Unit cannot exceed 30 characters.";
        }
        if (supplier != null && supplier.length() > 100) {
            return "Supplier cannot exceed 100 characters.";
        }
        return null;
    }

    private Integer parseNonNegativeInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseOptionalPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.compareTo(BigDecimal.ZERO) < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
