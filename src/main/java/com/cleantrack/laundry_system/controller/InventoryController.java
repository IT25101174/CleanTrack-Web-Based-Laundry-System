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
import com.cleantrack.laundry_system.service.InventoryStockService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private static final int MAX_USAGE_AMOUNT = 100_000;
    private static final String MANAGE_DENIED_MESSAGE = "Only the Branch Supervisor can manage inventory.";

    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogRepository auditLogRepository;
    private final SupplierRepository supplierRepository;
    private final SupplyOrderRepository supplyOrderRepository;
    private final InventoryStockService stockService;

    @Autowired
    public InventoryController(InventoryItemRepository inventoryItemRepository,
                               AuditLogRepository auditLogRepository,
                               SupplierRepository supplierRepository,
                               SupplyOrderRepository supplyOrderRepository,
                               InventoryStockService stockService) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.supplierRepository = supplierRepository;
        this.supplyOrderRepository = supplyOrderRepository;
        this.stockService = stockService;
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
        Map<String, Supplier> supplierMap = new LinkedHashMap<>();
        for (Supplier s : suppliers) {
            supplierMap.put(s.getSupplierName(), s);
        }

        model.addAttribute("items", items);
        model.addAttribute("lowStockItems", items.stream().filter(InventoryItem::isLowStock).toList());
        model.addAttribute("totalValue", totalValue);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("supplierMap", supplierMap);
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
                          @RequestParam(required = false) String usageBasis,
                          @RequestParam(required = false) String usageAmount,
                          @RequestParam(required = false) java.util.List<String> usageServices,
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
        if (validationError == null && inventoryItemRepository.existsByItemNameIgnoreCase(name)) {
            validationError = "An inventory item named " + name + " already exists.";
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

        UsageRule rule = parseUsageRule(usageBasis, usageAmount, usageServices);
        if (rule.error != null) {
            redirectAttributes.addFlashAttribute("error", rule.error);
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
        rule.applyTo(item);
        inventoryItemRepository.save(item);
        auditLogRepository.save(new AuditLog("Inventory item added: " + item.getItemName() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Item added successfully.");
        return "redirect:/inventory";
    }

    @PostMapping("/edit/{id}")
    public String editItem(@PathVariable Long id,
                           @RequestParam String itemName,
                           @RequestParam String quantity,
                           @RequestParam String lowStockThreshold,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String unit,
                           @RequestParam(required = false) String supplier,
                           @RequestParam(required = false) String unitPrice,
                           @RequestParam(required = false) String usageBasis,
                           @RequestParam(required = false) String usageAmount,
                           @RequestParam(required = false) java.util.List<String> usageServices,
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
        Integer parsedQuantity = parseNonNegativeInt(quantity);
        if (validationError == null && parsedQuantity == null) {
            validationError = "Quantity must be a whole number of zero or greater.";
        }
        Integer parsedThreshold = parseNonNegativeInt(lowStockThreshold);
        if (validationError == null && parsedThreshold == null) {
            validationError = "Low stock threshold must be a whole number of zero or greater.";
        }
        if (validationError == null) {
            validationError = validateTextLengths(categoryValue, unitValue, supplierValue);
        }
        if (validationError == null) {
            Optional<InventoryItem> sameName = inventoryItemRepository.findFirstByItemNameIgnoreCase(name);
            if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
                validationError = "An inventory item named " + name + " already exists.";
            }
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

        UsageRule rule = parseUsageRule(usageBasis, usageAmount, usageServices);
        if (rule.error != null) {
            redirectAttributes.addFlashAttribute("error", rule.error);
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

        int quantityBefore = item.getQuantity();
        item.setItemName(name);
        item.setQuantity(parsedQuantity);
        item.setLowStockThreshold(parsedThreshold);
        item.setCategory(categoryValue);
        item.setUnit(unitValue);
        item.setSupplier(resolvedSupplier);
        item.setUnitPrice(parsedPrice);
        rule.applyTo(item);
        item.setUpdatedAt(LocalDateTime.now());
        inventoryItemRepository.save(item);

        if (quantityBefore != parsedQuantity) {
            auditLogRepository.save(new AuditLog("Stock count for " + name + " set from " + quantityBefore
                    + " to " + parsedQuantity + " by " + user.getFullName()));
        }
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

        // Atomic deduction (and a single low-stock alert when the threshold is crossed) in the stock service.
        if (!stockService.consume(item, parsedAmount, "manual", user.getFullName())) {
            redirectAttributes.addFlashAttribute("error",
                    "Cannot consume more than available stock for " + item.getItemName());
            return "redirect:/inventory";
        }

        redirectAttributes.addFlashAttribute("success", "Successfully recorded usage of " + item.getItemName());
        return "redirect:/inventory";
    }

    // The automatic-use rule typed into the add/edit form.
    private static class UsageRule {
        String basis;
        Integer amount;
        String services;
        String error;

        void applyTo(InventoryItem item) {
            item.setUsageBasis(basis);
            item.setUsageAmount(amount);
            item.setUsageServices(services);
        }
    }

    // Reads and validates the usage rule. "Manual only" (or no choice) clears the rule.
    private UsageRule parseUsageRule(String basis, String amount, java.util.List<String> services) {
        UsageRule rule = new UsageRule();
        if (basis == null || basis.isBlank() || "MANUAL".equals(basis)) {
            rule.basis = "MANUAL";
            return rule;
        }
        if (!"PER_GARMENTS".equals(basis) && !"PER_ORDER".equals(basis)) {
            rule.error = "Choose a valid automatic-use option.";
            return rule;
        }
        Integer parsedAmount = null;
        try {
            if (amount != null && !amount.isBlank()) {
                int value = Integer.parseInt(amount.trim());
                if (value >= 1 && value <= MAX_USAGE_AMOUNT) {
                    parsedAmount = value;
                }
            }
        } catch (NumberFormatException ignored) {
            // handled below
        }
        if (parsedAmount == null) {
            rule.error = "Automatic use amount must be a whole number between 1 and " + MAX_USAGE_AMOUNT + ".";
            return rule;
        }
        java.util.List<String> allowed = java.util.List.of("WASH_ONLY", "WASH_IRON", "DRY_CLEAN");
        java.util.List<String> chosen = new java.util.ArrayList<>();
        if (services != null) {
            for (String service : services) {
                if (service != null && allowed.contains(service.trim()) && !chosen.contains(service.trim())) {
                    chosen.add(service.trim());
                }
            }
        }
        if (chosen.isEmpty()) {
            rule.error = "Select at least one service for the automatic use rule.";
            return rule;
        }
        rule.basis = basis;
        rule.amount = parsedAmount;
        rule.services = String.join(",", chosen);
        return rule;
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
