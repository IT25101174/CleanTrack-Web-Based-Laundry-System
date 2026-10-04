package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.AuditLog;
import com.cleantrack.laundry_system.model.Supplier;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.AuditLogRepository;
import com.cleantrack.laundry_system.repository.SupplierRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{7,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final SupplierRepository supplierRepository;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public SupplierController(SupplierRepository supplierRepository, AuditLogRepository auditLogRepository) {
        this.supplierRepository = supplierRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listSuppliers(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }

        model.addAttribute("suppliers", supplierRepository.findAllByOrderBySupplierNameAsc());
        model.addAttribute("user", user);
        model.addAttribute("canManage", canManage(user));
        return "supplier-list";
    }

    @PostMapping("/add")
    public String addSupplier(@RequestParam String supplierName,
                              @RequestParam(required = false) String contactPerson,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) String address,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage suppliers.");
            return "redirect:/suppliers";
        }

        String name = clean(supplierName);
        String contact = clean(contactPerson);
        String phoneValue = clean(phone);
        String emailValue = clean(email);
        String addressValue = clean(address);

        String validationError = validateSupplierFields(name, contact, phoneValue, emailValue, addressValue);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/suppliers";
        }

        if (supplierRepository.existsBySupplierNameIgnoreCase(name)) {
            redirectAttributes.addFlashAttribute("error", "A supplier named " + name + " already exists.");
            return "redirect:/suppliers";
        }

        Supplier supplier = new Supplier(name, contact, phoneValue, emailValue, addressValue);
        supplierRepository.save(supplier);
        auditLogRepository.save(new AuditLog("Supplier added: " + name + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Supplier added successfully.");
        return "redirect:/suppliers";
    }

    @PostMapping("/edit/{id}")
    public String editSupplier(@PathVariable Long id,
                               @RequestParam String supplierName,
                               @RequestParam(required = false) String contactPerson,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String email,
                               @RequestParam(required = false) String address,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage suppliers.");
            return "redirect:/suppliers";
        }

        String name = clean(supplierName);
        String contact = clean(contactPerson);
        String phoneValue = clean(phone);
        String emailValue = clean(email);
        String addressValue = clean(address);

        String validationError = validateSupplierFields(name, contact, phoneValue, emailValue, addressValue);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("error", validationError);
            return "redirect:/suppliers";
        }

        Optional<Supplier> existing = supplierRepository.findById(id);
        if (existing.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Supplier not found.");
            return "redirect:/suppliers";
        }

        Optional<Supplier> sameName = supplierRepository.findBySupplierNameIgnoreCase(name);
        if (sameName.isPresent() && !sameName.get().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "A supplier named " + name + " already exists.");
            return "redirect:/suppliers";
        }

        Supplier supplier = existing.get();
        supplier.setSupplierName(name);
        supplier.setContactPerson(contact);
        supplier.setPhone(phoneValue);
        supplier.setEmail(emailValue);
        supplier.setAddress(addressValue);
        supplier.setUpdatedAt(LocalDateTime.now());
        supplierRepository.save(supplier);
        auditLogRepository.save(new AuditLog("Supplier updated: " + name + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Supplier updated successfully.");
        return "redirect:/suppliers";
    }

    @PostMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null || isCustomer(user)) {
            return "redirect:/login";
        }
        if (!canManage(user)) {
            redirectAttributes.addFlashAttribute("error", "Only the Branch Supervisor can manage suppliers.");
            return "redirect:/suppliers";
        }

        // Supply orders keep a copy of the supplier name, so deleting a supplier never breaks an order.
        boolean found = supplierRepository.findById(id).map(supplier -> {
            supplierRepository.delete(supplier);
            auditLogRepository.save(new AuditLog("Supplier deleted: " + supplier.getSupplierName() + " by " + user.getFullName()));
            return true;
        }).orElse(false);

        redirectAttributes.addFlashAttribute(found ? "success" : "error",
                found ? "Supplier removed." : "Supplier not found.");
        return "redirect:/suppliers";
    }

    private boolean isCustomer(User user) {
        return user.getRole() != null && "CUSTOMER".equals(user.getRole().name());
    }

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

    // Limits match the column lengths in the Supplier entity.
    private String validateSupplierFields(String name, String contact, String phone, String email, String address) {
        if (name == null) {
            return "Supplier name cannot be empty.";
        }
        if (name.length() > 100) {
            return "Supplier name cannot exceed 100 characters.";
        }
        if (contact != null && contact.length() > 100) {
            return "Contact person cannot exceed 100 characters.";
        }
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            return "Phone number must be 7 to 30 characters and contain only digits, spaces, +, - and brackets.";
        }
        if (email != null) {
            if (email.length() > 100) {
                return "Email cannot exceed 100 characters.";
            }
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                return "Email address is not valid.";
            }
        }
        if (address != null && address.length() > 200) {
            return "Address cannot exceed 200 characters.";
        }
        return null;
    }
}