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

        // Orders whose payment is already made (PAID) or waiting for approval (PENDING_APPROVAL) cannot be edited
        java.util.Set<Long> paidOrderIds = new java.util.HashSet<>();
        java.util.Set<Long> paymentSubmittedOrderIds = new java.util.HashSet<>();
        for (Order o : orders) {
            for (com.cleantrack.laundry_system.model.Invoice invoice : invoiceRepository.findByOrderId(o.getId())) {
                if ("PAID".equals(invoice.getStatus())) {
                    paidOrderIds.add(o.getId());
                    paymentSubmittedOrderIds.add(o.getId());
                } else if ("PENDING_APPROVAL".equals(invoice.getStatus())) {
                    paymentSubmittedOrderIds.add(o.getId());
                }
            }
        }

        model.addAttribute("orders", orders);
        model.addAttribute("paidOrderIds", paidOrderIds);
        model.addAttribute("paymentSubmittedOrderIds", paymentSubmittedOrderIds);
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
                !isKnownService(order.getServiceType()) ||
                paymentMethod == null || paymentMethod.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please fill all required fields correctly.");
            return "redirect:/orders/new";
        }

        // Never trust an id sent from the form: it could overwrite an existing order
        order.setId(null);

        // For a bank transfer the receipt is checked and stored first, so a failure leaves no half-created order
        String receiptPath = null;
        if ("BANK_TRANSFER".equals(paymentMethod)) {
            if (bankReceipt == null || bankReceipt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Bank receipt is required for Bank Transfer.");
                return "redirect:/orders/new";
            }
            receiptPath = storeReceipt(bankReceipt);
            if (receiptPath == null) {
                redirectAttributes.addFlashAttribute("error",
                        "Could not upload the receipt. Please upload a PDF or an image (PNG, JPG, GIF or WEBP).");
                return "redirect:/orders/new";
            }
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
            
            // Workflow fix: Keep order out of queue until payment is approved
            savedOrder.setStatus("AWAITING_PAYMENT");
            orderRepository.save(savedOrder);
            invoice.setReceiptPath(receiptPath);
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

        if (!canAccess(user, order)) {
            redirectAttributes.addFlashAttribute("error", "You can only change your own orders.");
            return "redirect:/orders";
        }

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        if (isPaymentSubmitted(order.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment has already been made for this order, so it can no longer be edited.");
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

        if (!canAccess(user, existingOrder)) {
            redirectAttributes.addFlashAttribute("error", "You can only change your own orders.");
            return "redirect:/orders";
        }

        if (isProcessingOrBeyond(existingOrder.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be edited once it has entered processing.");
            return "redirect:/orders";
        }

        if (updatedOrder.getItemDescription() == null || updatedOrder.getItemDescription().trim().isEmpty()
                || updatedOrder.getQuantity() == null || updatedOrder.getQuantity() <= 0
                || !isKnownService(updatedOrder.getServiceType())) {
            redirectAttributes.addFlashAttribute("error", "Please fill all required fields correctly.");
            return "redirect:/orders/" + id + "/edit";
        }

        // Once the customer has paid (or uploaded a bank receipt), the price cannot change
        if (isPaymentSubmitted(id)) {
            redirectAttributes.addFlashAttribute("error",
                    "Payment has already been made for this order, so it can no longer be edited.");
            return "redirect:/orders";
        }
        java.util.List<com.cleantrack.laundry_system.model.Invoice> invoices = invoiceRepository.findByOrderId(id);

        // Update allowed fields
        existingOrder.setItemDescription(updatedOrder.getItemDescription().trim());
        existingOrder.setQuantity(updatedOrder.getQuantity());
        existingOrder.setServiceType(updatedOrder.getServiceType());
        orderRepository.save(existingOrder);

        // Keep the invoice in step with the new items and service
        for (com.cleantrack.laundry_system.model.Invoice invoice : invoices) {
            java.math.BigDecimal subtotal = priceFor(existingOrder.getServiceType())
                    .multiply(new java.math.BigDecimal(existingOrder.getQuantity()));
            java.math.BigDecimal tax = subtotal.multiply(new java.math.BigDecimal("0.10"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            invoice.setSubtotal(subtotal);
            invoice.setTaxAmount(tax);
            invoice.setTotalAmount(subtotal.add(tax));
            invoiceRepository.save(invoice);
        }

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

        if (!canAccess(user, order)) {
            redirectAttributes.addFlashAttribute("error", "You can only change your own orders.");
            return "redirect:/orders";
        }

        if (isProcessingOrBeyond(order.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Order cannot be cancelled once it has entered processing.");
            return "redirect:/orders";
        }

        for (com.cleantrack.laundry_system.model.Invoice invoice : invoiceRepository.findByOrderId(id)) {
            if ("PAID".equals(invoice.getStatus())) {
                redirectAttributes.addFlashAttribute("error",
                        "This order is already paid. Please contact the branch to cancel it.");
                return "redirect:/orders";
            }
        }

        order.setStatus("Cancelled");
        orderRepository.save(order);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Order cancelled: " + order.getTrackingId() + " by " + user.getFullName()));
        redirectAttributes.addFlashAttribute("success", "Order cancelled successfully!");

        return "redirect:/orders";
    }

    // An order can only be edited or cancelled while it is waiting: a new order, or one waiting for its payment
    // to be approved. Anything else (processing stages, completed, cancelled) is locked.
    private boolean isProcessingOrBeyond(String status) {
        if (status == null)
            return false;
        return !(status.equalsIgnoreCase("Order Placed") || status.equalsIgnoreCase("AWAITING_PAYMENT"));
    }

    // True when the order's invoice is paid or its bank transfer is waiting for approval
    private boolean isPaymentSubmitted(Long orderId) {
        for (com.cleantrack.laundry_system.model.Invoice invoice : invoiceRepository.findByOrderId(orderId)) {
            if ("PAID".equals(invoice.getStatus()) || "PENDING_APPROVAL".equals(invoice.getStatus())) {
                return true;
            }
        }
        return false;
    }

    // A customer may only change their own orders. Staff can change any order.
    private boolean canAccess(User user, Order order) {
        if (user.getRole() == null || !"CUSTOMER".equals(user.getRole().name())) {
            return true;
        }
        return order.getCustomer() != null && order.getCustomer().getId() != null
                && order.getCustomer().getId().equals(user.getId());
    }

    private boolean isKnownService(String serviceType) {
        return "WASH_ONLY".equals(serviceType) || "WASH_IRON".equals(serviceType) || "DRY_CLEAN".equals(serviceType);
    }

    private java.math.BigDecimal priceFor(String serviceType) {
        if ("WASH_IRON".equals(serviceType))
            return new java.math.BigDecimal("800.00");
        if ("DRY_CLEAN".equals(serviceType))
            return new java.math.BigDecimal("1200.00");
        return new java.math.BigDecimal("500.00");
    }

    // Saves a bank receipt in the "uploads" folder (served at /uploads/...). Only PDF and image files are accepted
    // and the stored name is cleaned, so a crafted file name cannot write outside the folder.
    // Returns the web path of the stored file, or null when the file is not accepted.
    private String storeReceipt(org.springframework.web.multipart.MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int slash = Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\'));
        String baseName = original.substring(slash + 1).replaceAll("[^A-Za-z0-9._-]", "_");
        int dot = baseName.lastIndexOf('.');
        String extension = dot < 0 ? "" : baseName.substring(dot + 1).toLowerCase();
        if (!java.util.List.of("pdf", "png", "jpg", "jpeg", "gif", "webp").contains(extension)) {
            return null;
        }
        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads");
            java.nio.file.Files.createDirectories(uploadDir);
            String storedName = System.currentTimeMillis() + "_" + baseName;
            java.nio.file.Files.copy(file.getInputStream(), uploadDir.resolve(storedName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + storedName;
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
