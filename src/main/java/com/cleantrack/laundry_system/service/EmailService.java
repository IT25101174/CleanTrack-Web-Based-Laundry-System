package com.cleantrack.laundry_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @Autowired
    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Async
    public void sendStatusEmail(String toEmail, String status, Long orderId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@cleantrack.com");
            message.setTo(toEmail);
            
            if ("PAID".equals(status)) {
                message.setSubject("Payment Approved - CleanTrack Laundry");
                message.setText("Great news!\n\nYour payment for Order #" + orderId + " has been approved.\nWe are now processing your laundry.\n\nThank you for choosing CleanTrack!");
            } else if ("REJECTED".equals(status)) {
                message.setSubject("Payment Rejected - CleanTrack Laundry");
                message.setText("Hello,\n\nUnfortunately, we were unable to verify your bank transfer for Order #" + orderId + ".\nPlease contact us to resolve this issue.\n\nThank you,\nCleanTrack Team");
            }
            
            emailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
