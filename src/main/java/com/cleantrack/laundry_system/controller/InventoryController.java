package com.cleantrack.laundry_system.controller;

import com.cleantrack.model.AuditLog;
import com.cleantrack.model.InventoryItem;
import com.cleantrack.model.User;
import com.cleantrack.repository.AuditLogRepository;
import com.cleantrack.repository.InventoryItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryItemRepository inventoryItemRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public InventoryController(InventoryItemRepository inventoryItemRepository, AuditLogRepository auditLogRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.auditLogRepository = auditLogRepository;
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

        model.addAttribute("items", items);
        model.addAttribute("totalValue", totalValue);
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
        if (user == null || isCustomer(user) || isCounterStaff(user)) {
            return "redirect:/login";
        }

        String validationError = validateItemFields(itemName, quantity, lowStockThreshold);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/inventory";
        }

        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        if (unitPrice != null && !unitPrice.isBlank() && parsedPrice == null) {
            redirectAttributes.addFlashAttribute("error", "Unit price must be a valid non-negative number.");
            return "redirect:/inventory";
        }

        InventoryItem item = new InventoryItem(
                itemName.trim(),
                parseNonNegativeInt(quantity),
                parseNonNegativeInt(lowStockThreshold),
                category, unit, supplier, parsedPrice);
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
        if (user == null || isCustomer(user) || isCounterStaff(user)) {
            return "redirect:/login";
        }

        if (itemName == null || itemName.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Item name cannot be empty.");
            return "redirect:/inventory";
        }
        if (itemName.trim().length() > 100) {
            redirectAttributes.addFlashAttribute("error", "Item name cannot exceed 100 characters.");
            return "redirect:/inventory";
        }
        Integer parsedThreshold = parseNonNegativeInt(lowStockThreshold);
        if (parsedThreshold == null) {
            redirectAttributes.addFlashAttribute("error", "Low stock threshold must be a whole number of zero or greater.");
            return "redirect:/inventory";
        }
        BigDecimal parsedPrice = parseOptionalPrice(unitPrice);
        if (unitPrice != null && !unitPrice.isBlank() && parsedPrice == null) {
            redirectAttributes.addFlashAttribute("error", "Unit price must be a valid non-negative number.");
            return "redirect:/inventory";
        }

        boolean found = inventoryItemRepository.findById(id).map(item -> {
            item.setItemName(itemName.trim());
            item.setLowStockThreshold(parsedThreshold);
            item.setCategory(category);
            item.setUnit(unit);
            item.setSupplier(supplier);
            item.setUnitPrice(parsedPrice);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            auditLogRepository.save(new AuditLog("Updated inventory details for " + itemName + " by " + user.getFullName()));
            return true;
        }).orElse(false);

        redirectAttributes.addFlashAttribute(found ? "success" : "error",
                found ? "Inventory item updated successfully." : "Item not found.");
        return "redirect:/inventory";
    }

    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user) || isCounterStaff(user)) {
            return "redirect:/login";
        }

        boolean found = inventoryItemRepository.findById(id).map(item -> {
            inventoryItemRepository.delete(item);
            auditLogRepository.save(new AuditLog("Deleted inventory item " + item.getItemName() + " by " + user.getFullName()));
            return true;
        }).orElse(false);

        redirectAttributes.addFlashAttribute(found ? "success" : "error",
                found ? "Inventory item removed." : "Item not found.");
        return "redirect:/inventory";
    }

    @PostMapping("/restock/{id}")
    public String restockItem(@PathVariable Long id,
                              @RequestParam String amount,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user) || isCounterStaff(user)) {
            return "redirect:/login";
        }

        Integer parsedAmount = parseNonNegativeInt(amount);
        if (parsedAmount == null || parsedAmount == 0) {
            redirectAttributes.addFlashAttribute("error", "Restock amount must be a whole number greater than zero.");
            return "redirect:/inventory";
        }

        boolean found = inventoryItemRepository.findById(id).map(item -> {
            item.setQuantity(item.getQuantity() + parsedAmount);
            item.setUpdatedAt(LocalDateTime.now());
            inventoryItemRepository.save(item);
            auditLogRepository.save(new AuditLog("Restocked " + parsedAmount + " units of " + item.getItemName() + " by " + user.getFullName()));
            return true;
        }).orElse(false);

        redirectAttributes.addFlashAttribute(found ? "success" : "error",
                found ? "Successfully restocked item." : "Item not found.");
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

        inventoryItemRepository.findById(id).ifPresentOrElse(item -> {
            if (item.getQuantity() < parsedAmount) {
                redirectAttributes.addFlashAttribute("error", "Cannot consume more than available stock for " + item.getItemName());
            } else {
                item.setQuantity(item.getQuantity() - parsedAmount);
                item.setUpdatedAt(LocalDateTime.now());
                inventoryItemRepository.save(item);
                auditLogRepository.save(new AuditLog("Consumed " + parsedAmount + " units of " + item.getItemName() + " by " + user.getFullName()));
                redirectAttributes.addFlashAttribute("success", "Successfully recorded usage of " + item.getItemName());
            }
        }, () -> redirectAttributes.addFlashAttribute("error", "Item not found."));

        return "redirect:/inventory";
    }

    private boolean isCustomer(User user) {
        return user.getRole() != null && "CUSTOMER".equals(user.getRole().name());
    }

    private boolean isCounterStaff(User user) {
        return user.getRole() != null && "COUNTER_STAFF".equals(user.getRole().name());
    }

    private String validateItemFields(String itemName, String quantity, String lowStockThreshold) {
        if (itemName == null || itemName.isBlank()) {
            return "Item name cannot be empty.";
        }
        if (itemName.trim().length() > 100) {
            return "Item name cannot exceed 100 characters.";
        }
        if (parseNonNegativeInt(quantity) == null) {
            return "Quantity must be a whole number of zero or greater.";
        }
        if (parseNonNegativeInt(lowStockThreshold) == null) {
            return "Low stock threshold must be a whole number of zero or greater.";
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
