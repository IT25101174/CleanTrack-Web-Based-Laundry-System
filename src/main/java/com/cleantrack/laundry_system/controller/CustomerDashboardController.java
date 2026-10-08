package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.Order;
import com.cleantrack.laundry_system.model.Invoice;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.OrderRepository;
import com.cleantrack.laundry_system.repository.InvoiceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CustomerDashboardController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @GetMapping("/customer-dashboard")
    public String showCustomerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null || !"CUSTOMER".equals(user.getRole().name())) {
            return "redirect:/login";
        }
        
        model.addAttribute("user", user);

        // Fetch user's orders
        List<Order> userOrders = orderRepository.findByCustomerId(user.getId());
        
        List<Order> activeOrders = userOrders.stream()
            .filter(o -> !"COMPLETED".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
            .collect(Collectors.toList());
            
        long completedOrdersCount = userOrders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()))
            .count();
            
        // Total Items Cleaned
        long totalItemsCleaned = 0;
        List<Invoice> allInvoices = new java.util.ArrayList<>();
        
        for (Order order : userOrders) {
            List<Invoice> invoices = invoiceRepository.findByOrderId(order.getId());
            allInvoices.addAll(invoices);
            if ("COMPLETED".equals(order.getStatus()) && order.getQuantity() != null) {
                totalItemsCleaned += order.getQuantity();
            }
        }
        
        // Sort invoices to get recent ones
        allInvoices.sort((i1, i2) -> i2.getGeneratedAt().compareTo(i1.getGeneratedAt()));
        List<Invoice> recentInvoices = allInvoices.stream().limit(3).collect(Collectors.toList());

        model.addAttribute("activeOrdersCount", activeOrders.size());
        model.addAttribute("completedOrdersCount", completedOrdersCount);
        model.addAttribute("totalItemsCleaned", totalItemsCleaned);
        model.addAttribute("activeOrder", activeOrders.isEmpty() ? null : activeOrders.get(0));
        model.addAttribute("recentInvoices", recentInvoices);

        return "customer-dashboard";
    }
}
