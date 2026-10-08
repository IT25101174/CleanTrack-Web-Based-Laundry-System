package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.repository.ComplaintRepository;
import com.cleantrack.laundry_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;
    private final com.cleantrack.laundry_system.repository.UserRepository userRepository;
    private final com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository;

    @Autowired
    public ComplaintController(ComplaintRepository complaintRepository, OrderRepository orderRepository, com.cleantrack.laundry_system.repository.UserRepository userRepository, com.cleantrack.laundry_system.repository.AuditLogRepository auditLogRepository) {
        this.complaintRepository = complaintRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String listComplaints(HttpSession session, Model model) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        java.util.List<com.cleantrack.laundry_system.model.Complaint> complaints;
        
        // PRIVACY ENFORCEMENT
        if ("CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            complaints = complaintRepository.findByCustomerIdOrderByCreatedAtDesc(user.getId());
            // Provide orders for the dropdown
            model.addAttribute("myOrders", orderRepository.findByCustomerId(user.getId()));
        } else if ("ADMIN".equals(user.getRole() != null ? user.getRole().name() : null) || "BRANCH_SUPERVISOR".equals(user.getRole() != null ? user.getRole().name() : null)) {
            complaints = complaintRepository.findAllByOrderByCreatedAtDesc();
            model.addAttribute("staffMembers", userRepository.findByRoleNot(com.cleantrack.laundry_system.model.Role.CUSTOMER));
        } else {
            // Regular staff only see tickets assigned to them or unassigned
            complaints = complaintRepository.findAllByOrderByCreatedAtDesc(); // For MVP, let them see all, but you can filter this.
            model.addAttribute("staffMembers", userRepository.findByRoleNot(com.cleantrack.laundry_system.model.Role.CUSTOMER));
        }
        
        model.addAttribute("complaints", complaints);
        model.addAttribute("user", user);
        return "complaint-form";
    }

    @PostMapping("/submit")
    public String submitComplaint(@RequestParam(required = false) Long orderId,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String issueDescription,
                                  HttpSession session,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || !"CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        // Input Validation
        if (orderId == null) {
            redirectAttributes.addFlashAttribute("error", "Please select an order to submit a complaint against.");
            return "redirect:/complaints";
        }
        if (category == null || category.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select an issue category.");
            return "redirect:/complaints";
        }
        if (issueDescription == null || issueDescription.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please provide a description of the issue.");
            return "redirect:/complaints";
        }
        if (issueDescription.length() > 1000) {
            redirectAttributes.addFlashAttribute("error", "Description is too long. Maximum 1000 characters allowed.");
            return "redirect:/complaints";
        }
        
        // Ownership Validation
        java.util.Optional<com.cleantrack.laundry_system.model.Order> optOrder = orderRepository.findById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getCustomer() == null || !optOrder.get().getCustomer().getId().equals(user.getId())) {
            redirectAttributes.addFlashAttribute("error", "Invalid order or you do not have permission to complain about this order.");
            return "redirect:/complaints";
        }
        
        com.cleantrack.laundry_system.model.Complaint complaint = new com.cleantrack.laundry_system.model.Complaint(optOrder.get(), user, category, issueDescription, "OPEN");
        complaintRepository.save(complaint);
        auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Complaint submitted for Order ID: " + orderId + " by " + user.getFullName()));
        
        redirectAttributes.addFlashAttribute("success", "Your complaint has been submitted successfully. We will resolve it shortly.");
        return "redirect:/complaints";
    }

    @PostMapping("/assign/{id}")
    public String assignComplaint(@PathVariable Long id, @RequestParam Long staffId, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        java.util.Optional<com.cleantrack.laundry_system.model.Complaint> optComplaint = complaintRepository.findById(id);
        java.util.Optional<com.cleantrack.laundry_system.model.User> optStaff = userRepository.findById(staffId);
        
        if (optComplaint.isPresent() && optStaff.isPresent()) {
            com.cleantrack.laundry_system.model.Complaint c = optComplaint.get();
            c.setAssignedStaff(optStaff.get());
            if ("OPEN".equals(c.getStatus())) {
                c.setStatus("IN_PROGRESS");
            }
            complaintRepository.save(c);
            auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Complaint ID " + id + " assigned to " + optStaff.get().getFullName() + " by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Ticket assigned to " + optStaff.get().getFullName());
        }
        return "redirect:/complaints";
    }

    @PostMapping("/resolve/{id}")
    public String resolveComplaint(@PathVariable Long id, @RequestParam String resolutionNotes, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        java.util.Optional<com.cleantrack.laundry_system.model.Complaint> optComplaint = complaintRepository.findById(id);
        if (optComplaint.isPresent()) {
            com.cleantrack.laundry_system.model.Complaint c = optComplaint.get();
            if (resolutionNotes == null || resolutionNotes.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Resolution notes are required to close a ticket.");
                return "redirect:/complaints";
            }
            c.setResolutionNotes(resolutionNotes);
            c.setStatus("RESOLVED");
            c.setResolvedAt(java.time.LocalDateTime.now());
            complaintRepository.save(c);
            auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Complaint ID " + id + " resolved by " + user.getFullName()));
            redirectAttributes.addFlashAttribute("success", "Ticket has been resolved.");
        }
        return "redirect:/complaints";
    }
    
    @PostMapping("/archive/{id}")
    public String archiveComplaint(@PathVariable Long id, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || "CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        java.util.Optional<com.cleantrack.laundry_system.model.Complaint> optComplaint = complaintRepository.findById(id);
        if (optComplaint.isPresent()) {
            com.cleantrack.laundry_system.model.Complaint c = optComplaint.get();
            c.setStatus("ARCHIVED");
            complaintRepository.save(c);
            redirectAttributes.addFlashAttribute("success", "Ticket has been archived.");
        }
        return "redirect:/complaints";
    }

    @PostMapping("/delete/{id}")
    public String deleteComplaint(@PathVariable Long id, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || !"CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        java.util.Optional<com.cleantrack.laundry_system.model.Complaint> optComplaint = complaintRepository.findById(id);
        if (optComplaint.isPresent()) {
            com.cleantrack.laundry_system.model.Complaint c = optComplaint.get();
            // Ensure customer owns the complaint
            if (c.getCustomer().getId().equals(user.getId())) {
                complaintRepository.delete(c);
                auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Complaint ID " + id + " deleted by customer " + user.getFullName()));
                redirectAttributes.addFlashAttribute("success", "Your ticket has been deleted.");
            }
        }
        return "redirect:/complaints";
    }

    @PostMapping("/rate/{id}")
    public String rateComplaint(@PathVariable Long id, @RequestParam Integer rating, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        com.cleantrack.laundry_system.model.User user = (com.cleantrack.laundry_system.model.User) session.getAttribute("user");
        if (user == null || !"CUSTOMER".equals(user.getRole() != null ? user.getRole().name() : null)) {
            return "redirect:/login";
        }
        
        java.util.Optional<com.cleantrack.laundry_system.model.Complaint> optComplaint = complaintRepository.findById(id);
        if (optComplaint.isPresent()) {
            com.cleantrack.laundry_system.model.Complaint c = optComplaint.get();
            if (c.getCustomer().getId().equals(user.getId()) && "RESOLVED".equals(c.getStatus()) && c.getSatisfactionRating() == null) {
                if (rating >= 1 && rating <= 5) {
                    c.setSatisfactionRating(rating);
                    complaintRepository.save(c);
                    auditLogRepository.save(new com.cleantrack.laundry_system.model.AuditLog("Complaint ID " + id + " rated " + rating + " stars by customer " + user.getFullName()));
                    redirectAttributes.addFlashAttribute("success", "Thank you for your feedback!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "Invalid rating. Must be between 1 and 5.");
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "You cannot rate this ticket.");
            }
        }
        return "redirect:/complaints";
    }
}
