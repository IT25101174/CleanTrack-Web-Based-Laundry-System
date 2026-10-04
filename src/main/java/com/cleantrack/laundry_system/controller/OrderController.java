package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.Order;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final com.cleantrack.laundry_system.repository.InvoiceRepository invoiceRepository;
    private final com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository;

    @Autowired
    public OrderController(OrderRepository orderRepository,
            com.cleantrack.laundry_system.repository.InvoiceRepository invoiceRepository,
            com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository) {
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
        this.auditLogRepository = auditLogRepository;
    }

    private User getSessionUser(HttpSession session) {
        return (User) session.getAttribute("user");
    }

    @GetMapping
    public String listOrders(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        List<Order> orders;
        if ("CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            orders = orderRepository.findByCustomerId(user.getId());
        } else {
            orders = orderRepository.findAll();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("user", user);
        return "order-list";
    }

    @GetMapping("/new")
    public String showOrderForm(HttpSession session, Model model) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        model.addAttribute("order", new Order());
        model.addAttribute("user", user);
        return "order-form";
    }

    @PostMapping("/new")
    public String createOrder(@ModelAttribute Order order,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile bankReceipt,
            HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        // Basic validation
        if (order.getItemDescription() == null || order.getItemDescription().isEmpty() ||
                order.getQuantity() == null || order.getQuantity() <= 0 ||
                order.getServiceType() == null || order.getServiceType().isEmpty() ||
                paymentMethod == null || paymentMethod.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill all required fields correctly.");
            return "redirect:/orders/new";
        }

        order.setTrackingId("CT-" + System.currentTimeMillis());
        order.setStatus("Order Placed");

        if ("CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            order.setCustomer(user);
            order.setCustomerName(user.getFullName());
        } else {
            if (order.getCustomer() == null || order.getCustomer().getId() == null) {
                order.setCustomer(user);
            }
            order.setCounterStaff(user);
            order.setCustomerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "Unknown");
        }

        // Save order first to get ID
        Order savedOrder = orderRepository.save(order);

        // Generate Invoice automatically based on new integrated payment flow
        java.math.BigDecimal basePrice = new java.math.BigDecimal("500.00");
        if ("WASH_IRON".equals(savedOrder.getServiceType()))
            basePrice = new java.math.BigDecimal("800.00");
        if ("DRY_CLEAN".equals(savedOrder.getServiceType()))
            basePrice = new java.math.BigDecimal("1200.00");

        int qty = savedOrder.getQuantity();
        java.math.BigDecimal subtotal = basePrice.multiply(new java.math.BigDecimal(qty));
        java.math.BigDecimal taxAmount = subtotal.multiply(new java.math.BigDecimal("0.10")).setScale(2,
                java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal totalAmount = subtotal.add(taxAmount);

        com.cleantrack.laundry_system.model.Invoice invoice = new com.cleantrack.laundry_system.model.Invoice(savedOrder.getId(), subtotal, taxAmount,
                totalAmount, "UNPAID");
        invoice.setPaymentMethod(paymentMethod);

        if ("BANK_TRANSFER".equals(paymentMethod)) {
            invoice.setStatus("PENDING_APPROVAL");
            invoice.setAmountPaid(java.math.BigDecimal.ZERO);

            // Handle file upload
            if (bankReceipt != null && !bankReceipt.isEmpty()) {
                try {
                    String fileName = System.currentTimeMillis() + "_" + bankReceipt.getOriginalFilename();
                    java.nio.file.Path uploadPath = java.nio.file.Paths.get("src/main/resources/static/uploads/");
                    if (!java.nio.file.Files.exists(uploadPath)) {
                        java.nio.file.Files.createDirectories(uploadPath);
                    }
                    java.nio.file.Files.copy(bankReceipt.getInputStream(), uploadPath.resolve(fileName),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    invoice.setReceiptPath("/uploads/" + fileName);
                } catch (java.io.IOException e) {
                    redirectAttributes.addFlashAttribute("error", "Failed to upload bank receipt.");
                    return "redirect:/orders/new";
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Bank receipt is required for Bank Transfer.");
                return "redirect:/orders/new";
            }
        }

        // Save the generated invoice
        invoiceRepository.save(invoice);
        
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order created: " + savedOrder.getTrackingId() + " by " + user.getFullName()));

        redirectAttributes.addFlashAttribute("success", "Order created! Please wait for admin approval on your Bank Transfer. Tracking ID: " + savedOrder.getTrackingId());

        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order order = orderOpt.get();

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        model.addAttribute("order", order);
        model.addAttribute("user", user);
        return "order-form";
    }

    @PostMapping("/{id}/edit")
    public String updateOrder(@PathVariable Long id, @ModelAttribute Order updatedOrder, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order existingOrder = orderOpt.get();

        if (isProcessingOrBeyond(existingOrder.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        // Update allowed fields
        existingOrder.setItemDescription(updatedOrder.getItemDescription());
        existingOrder.setQuantity(updatedOrder.getQuantity());
        existingOrder.setServiceType(updatedOrder.getServiceType());

        orderRepository.save(existingOrder);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order updated: " + existingOrder.getTrackingId() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Order updated successfully!");

        return "redirect:/orders";
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = getSessionUser(session);
        if (user == null)
            return "redirect:/login";

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return "redirect:/orders";
        }
        Order order = orderOpt.get();

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be cancelled once it has entered processing.");
            return "redirect:/orders";
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order cancelled: " + order.getTrackingId() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Order cancelled successfully!");

        return "redirect:/orders";
    }

    private boolean isProcessingOrBeyond(String status) {
        if (status == null)
            return false;
        // Assume anything other than "Order Placed" or "PENDING" is processing or
        // beyond.
        // Or explicitly check for "WASHING", "DRYING", "IRONING", "READY FOR
        // COLLECTION", "COMPLETED"
        return status.equalsIgnoreCase("WASHING") ||
                status.equalsIgnoreCase("DRYING") ||
                status.equalsIgnoreCase("IRONING") ||
                status.equalsIgnoreCase("READY FOR COLLECTION") ||
                status.equalsIgnoreCase("COMPLETED");
    }
}
