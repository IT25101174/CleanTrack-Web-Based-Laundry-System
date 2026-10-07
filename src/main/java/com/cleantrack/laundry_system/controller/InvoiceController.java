package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.Invoice;
import com.cleantrack.laundry_system.model.Order;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.InvoiceRepository;
import com.cleantrack.laundry_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository;

    @Autowired
    public InvoiceController(InvoiceRepository invoiceRepository, OrderRepository orderRepository, com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listInvoices(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        List<Invoice> invoices = new ArrayList<>();
        if ("CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            // FIX: Changed from findByCustomerName to findByCustomerId
            List<Order> userOrders = orderRepository.findByCustomerId(user.getId());
            for (Order order : userOrders) {
                invoices.addAll(invoiceRepository.findByOrderId(order.getId()));
            }
        } else {
            invoices = invoiceRepository.findAll();
        }
        
        model.addAttribute("invoices", invoices);
        model.addAttribute("user", user);
        return "invoice-view";
    }

    @PostMapping("/generate/{orderId}")
    public String generateInvoice(@PathVariable Long orderId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        if (orderId == null) {
            return "redirect:/invoices?error=InvalidOrder";
        }
        
        // Ensure invoice doesn't already exist for this order
        List<Invoice> existingInvoices = invoiceRepository.findByOrderId(orderId);
        if (!existingInvoices.isEmpty()) {
            return "redirect:/invoices?error=InvoiceAlreadyExists";
        }
        
        Optional<Order> optOrder = orderRepository.findById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/invoices?error=OrderNotFound";
        }
        
        Order order = optOrder.get();
        
        // Basic Pricing Logic: Base * Quantity
        BigDecimal basePrice = new BigDecimal("500.00"); // Default WASH_ONLY
        if ("WASH_IRON".equals(order.getServiceType())) basePrice = new BigDecimal("800.00");
        if ("DRY_CLEAN".equals(order.getServiceType())) basePrice = new BigDecimal("1200.00");
        
        int qty = order.getQuantity() != null && order.getQuantity() > 0 ? order.getQuantity() : 1;
        BigDecimal subtotal = basePrice.multiply(new BigDecimal(qty));
        
        // 10% Tax
        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);
        
        Invoice invoice = new Invoice(orderId, subtotal, taxAmount, totalAmount, "UNPAID");
        invoiceRepository.save(invoice);
        
        // Update order status if it's still new
        if ("NEW".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            orderRepository.save(order);
        }
        
        return "redirect:/invoices";
    }

    @PostMapping("/pay/{id}")
    public String recordPayment(@PathVariable Long id, 
                                @RequestParam(required = false) BigDecimal amountPaid, 
                                @RequestParam(required = false) String paymentMethod, 
                                HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        Optional<Invoice> optInvoice = invoiceRepository.findById(id);
        if (optInvoice.isPresent()) {
            Invoice invoice = optInvoice.get();
            
            if ("PAID".equals(invoice.getStatus())) {
                 return "redirect:/invoices?error=AlreadyPaid";
            }
            
            if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
                 return "redirect:/invoices?error=InvalidPaymentMethod";
            }
            
            // Validate payment
            if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                if (amountPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                    invoice.setAmountPaid(amountPaid);
                    invoice.setPaymentMethod(paymentMethod);
                    invoice.setStatus("PAID");
                    invoiceRepository.save(invoice);
                    
                    // Workflow Fix: Push order to queue when paid
                    Optional<Order> optOrder = orderRepository.findById(invoice.getOrderId());
                    if (optOrder.isPresent()) {
                        Order order = optOrder.get();
                        if ("AWAITING_PAYMENT".equals(order.getStatus()) || "Order Placed".equals(order.getStatus())) {
                            order.setStatus("Order Placed");
                            orderRepository.save(order);
                        }
                    }
                    
                    auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Invoice paid for Order ID: " + invoice.getOrderId() + " by " + user.getFullName()));
                } else {
                    return "redirect:/invoices?error=InsufficientPayment";
                }
            } else {
                return "redirect:/invoices?error=InvalidAmount";
            }
        } else {
            return "redirect:/invoices?error=InvoiceNotFound";
        }
        
        return "redirect:/invoices";
    }

    @PostMapping("/approve-bank-transfer/{id}")
    public String approveBankTransfer(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        Optional<Invoice> optInvoice = invoiceRepository.findById(id);
        if (optInvoice.isPresent()) {
            Invoice invoice = optInvoice.get();
            if ("PAID".equals(invoice.getStatus())) {
                return "redirect:/invoices?error=AlreadyPaid";
            }
            if ("PENDING_APPROVAL".equals(invoice.getStatus())) {
                invoice.setStatus("PAID");
                invoice.setAmountPaid(invoice.getTotalAmount());
                invoiceRepository.save(invoice);
                
                // Workflow Fix: Push order to queue when bank transfer is approved
                Optional<Order> optOrder = orderRepository.findById(invoice.getOrderId());
                if (optOrder.isPresent()) {
                    Order order = optOrder.get();
                    if ("AWAITING_PAYMENT".equals(order.getStatus()) || "Order Placed".equals(order.getStatus())) {
                        order.setStatus("Order Placed");
                        orderRepository.save(order);
                    }
                }
                
                auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Bank transfer approved for Invoice ID: " + invoice.getId() + " by " + user.getFullName()));
            } else {
                return "redirect:/invoices?error=InvalidStatusForApproval";
            }
        } else {
            return "redirect:/invoices?error=InvoiceNotFound";
        }
        
        return "redirect:/invoices";
    }

    @PostMapping("/delete/{id}")
    public String deleteInvoice(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"ADMIN".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }

        Optional<Invoice> optInvoice = invoiceRepository.findById(id);
        if (optInvoice.isPresent()) {
            invoiceRepository.delete(optInvoice.get());
            auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Invoice ID: " + id + " was deleted by " + user.getFullName()));
        }
        
        return "redirect:/invoices";
    }
}
