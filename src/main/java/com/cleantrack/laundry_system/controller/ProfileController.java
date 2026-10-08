package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/profile/edit")
    public String editProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // Fetch fresh user from DB
        Optional<User> freshUser = userRepository.findById(user.getId());
        if (freshUser.isPresent()) {
            model.addAttribute("user", freshUser.get());
        } else {
            model.addAttribute("user", user);
        }
        
        return "profile-edit";
    }
    
    @PostMapping("/profile/edit")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String address,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        Optional<User> optUser = userRepository.findById(user.getId());
        if (optUser.isPresent()) {
            User dbUser = optUser.get();
            dbUser.setFullName(fullName);
            dbUser.setEmail(email);
            dbUser.setPhone(phone);
            dbUser.setAddress(address);
            
            userRepository.save(dbUser);
            
            // Update session
            session.setAttribute("user", dbUser);
        }
        
        // Redirect back to dashboard based on role
        if (user.getRole() != null && "CUSTOMER".equals(user.getRole().name())) {
            return "redirect:/customer-dashboard";
        }
        
        return "redirect:/dashboard";
    }
}
