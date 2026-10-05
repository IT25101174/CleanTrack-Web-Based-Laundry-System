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
                          jakarta.servlet.http.HttpServletRequest request,
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

        FormProblem problem = checkItemForm(null, itemName, quantity, lowStockThreshold, category, unit,
                supplier, unitPrice, usageBasis, usageAmount, usageServices);
        if (problem != null) {
            return formError(redirectAttributes, request, "add", problem.message);
        }

        String name = clean(itemName);
        String categoryValue = clean(category);
        String unitValue = clean(unit);
        String supplierValue = clean(supplier);
        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        UsageRule rule = parseUsageRule(usageBasis, usageAmount, usageServices);

        // The supplier was checked above: it is one registered under Suppliers.
        String resolvedSupplier = supplierValue == null ? null
                : supplierRepository.findBySupplierNameIgnoreCase(supplierValue)
                        .map(Supplier::getSupplierName).orElse(null);

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
                           jakarta.servlet.http.HttpServletRequest request,
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

        FormProblem problem = checkItemForm(id, itemName, quantity, lowStockThreshold, category, unit,
                supplier, unitPrice, usageBasis, usageAmount, usageServices);
        if (problem != null) {
            return formError(redirectAttributes, request, "edit-" + id, problem.message);
        }

        String name = clean(itemName);
        String categoryValue = clean(category);
        String unitValue = clean(unit);
        String supplierValue = clean(supplier);
        Integer parsedQuantity = parseNonNegativeInt(quantity);
        Integer parsedThreshold = parseNonNegativeInt(lowStockThreshold);
        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        UsageRule rule = parseUsageRule(usageBasis, usageAmount, usageServices);

        // A changed supplier is a registered one (checked above). An unchanged value is kept as it is,
        // so items created before supplier management existed can still be edited.
        String resolvedSupplier = supplierValue;
        if (supplierValue != null && !supplierValue.equalsIgnoreCase(item.getSupplier())) {
            resolvedSupplier = supplierRepository.findBySupplierNameIgnoreCase(supplierValue)
                    .map(Supplier::getSupplierName).orElse(supplierValue);
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

    // A form error is returned to the same dialog: the message and the entered values are kept in flash
    // attributes, and the page re-opens the dialog with the values filled in and the message inside it.
    private String formError(RedirectAttributes redirectAttributes,
                             jakarta.servlet.http.HttpServletRequest request,
                             String target, String message) {
        Map<String, List<String>> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, value) -> values.put(key, new java.util.ArrayList<>(java.util.Arrays.asList(value))));
        redirectAttributes.addFlashAttribute("formError", message);
        redirectAttributes.addFlashAttribute("formTarget", target);
        redirectAttributes.addFlashAttribute("formValues", values);
        return "redirect:/inventory";
    }

    // One problem found in the add/edit form: which field it belongs to and the message to show beside it.
    private static class FormProblem {
        final String field;
        final String message;

        FormProblem(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }

    // Checks the add (id == null) or edit (id = item id) form and returns the first problem, or null when valid.
    // Used by the add and edit actions and by the /inventory/check endpoint that the dialogs call, so the rules
    // are written once.
    private FormProblem checkItemForm(Long id, String itemName, String quantity, String lowStockThreshold,
                                      String category, String unit, String supplier, String unitPrice,
                                      String usageBasis, String usageAmount, java.util.List<String> usageServices) {
        String name = clean(itemName);
        String nameError = validateName(name);
        if (nameError != null) {
            return new FormProblem("itemName", nameError);
        }
        if (parseNonNegativeInt(quantity) == null) {
            return new FormProblem("quantity", "Quantity must be a whole number of zero or greater.");
        }
        if (parseNonNegativeInt(lowStockThreshold) == null) {
            return new FormProblem("lowStockThreshold", "Low stock threshold must be a whole number of zero or greater.");
        }
        String categoryValue = clean(category);
        String unitValue = clean(unit);
        String supplierValue = clean(supplier);
        // Limits match the column lengths in InventoryItem (category = 50, unit = 30, supplier = 100).
        if (categoryValue != null && categoryValue.length() > 50) {
            return new FormProblem("category", "Category cannot exceed 50 characters.");
        }
        if (unitValue != null && unitValue.length() > 30) {
            return new FormProblem("unit", "Unit cannot exceed 30 characters.");
        }
        if (supplierValue != null && supplierValue.length() > 100) {
            return new FormProblem("supplier", "Supplier cannot exceed 100 characters.");
        }
        Optional<InventoryItem> sameName = inventoryItemRepository.findFirstByItemNameIgnoreCase(name);
        if (sameName.isPresent() && (id == null || !sameName.get().getId().equals(id))) {
            return new FormProblem("itemName", "An inventory item named " + name + " already exists.");
        }
        if (unitPrice != null && !unitPrice.isBlank() && parseOptionalPrice(unitPrice) == null) {
            return new FormProblem("unitPrice", "Unit price must be a valid non-negative number.");
        }
        UsageRule rule = parseUsageRule(usageBasis, usageAmount, usageServices);
        if (rule.error != null) {
            String field = rule.error.contains("service") ? "usageServices"
                    : rule.error.contains("amount") ? "usageAmount" : "usageBasis";
            return new FormProblem(field, rule.error);
        }
        if (supplierValue != null) {
            // A new item, or a changed supplier, must use a registered supplier. An unchanged value is kept.
            String currentSupplier = null;
            if (id != null) {
                Optional<InventoryItem> current = inventoryItemRepository.findById(id);
                currentSupplier = current.map(InventoryItem::getSupplier).orElse(null);
            }
            boolean unchanged = currentSupplier != null && supplierValue.equalsIgnoreCase(currentSupplier);
            if (!unchanged && supplierRepository.findBySupplierNameIgnoreCase(supplierValue).isEmpty()) {
                return new FormProblem("supplier",
                        "Supplier \"" + supplierValue + "\" does not exist. Add it under Suppliers first.");
            }
        }
        return null;
    }

    // Called by the add/edit dialogs before they submit, so a problem is shown beside the field without
    // reloading the page. Returns {} when the form is valid, or {"field": ..., "message": ...}.
    @PostMapping("/check")
    @ResponseBody
    public Map<String, String> checkForm(@RequestParam(required = false) Long id,
                                         @RequestParam(required = false) String itemName,
                                         @RequestParam(required = false) String quantity,
                                         @RequestParam(required = false) String lowStockThreshold,
                                         @RequestParam(required = false) String category,
                                         @RequestParam(required = false) String unit,
                                         @RequestParam(required = false) String supplier,
                                         @RequestParam(required = false) String unitPrice,
                                         @RequestParam(required = false) String usageBasis,
                                         @RequestParam(required = false) String usageAmount,
                                         @RequestParam(required = false) java.util.List<String> usageServices,
                                         HttpSession session) {
        Map<String, String> result = new LinkedHashMap<>();
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user) || !canManage(user)) {
            return result; // the normal submit explains the access problem
        }
        FormProblem problem = checkItemForm(id, itemName, quantity, lowStockThreshold, category, unit,
                supplier, unitPrice, usageBasis, usageAmount, usageServices);
        if (problem != null) {
            result.put("field", problem.field);
            result.put("message", problem.message);
        }
        return result;
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
        if (!com.cleantrack.laundry_system.strategy.UsageStrategyFactory.isKnown(basis)) {
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
