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
            @RequestParam(required = false) String address,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Optional<User> optUser = userRepository.findById(user.getId());
        if (optUser.isPresent()) {
            User dbUser = optUser.get();

            String name = fullName == null ? "" : fullName.trim();
            String mail = email == null ? "" : email.trim();
            String phoneNumber = phone == null ? "" : phone.trim();
            String addressValue = address == null ? "" : address.trim();

            String problem = null;
            if (name.isEmpty() || name.length() > 100) {
                problem = "Full name is required and cannot be longer than 100 characters.";
            } else if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || mail.length() > 100) {
                problem = "Please provide a valid email address.";
            } else if (phoneNumber.isEmpty() || phoneNumber.length() > 20) {
                problem = "Phone number is required and cannot be longer than 20 characters.";
            } else if (addressValue.length() > 255) {
                problem = "Address cannot be longer than 255 characters.";
            } else {
                Optional<User> sameEmail = userRepository.findByEmail(mail);
                if (sameEmail.isPresent() && !sameEmail.get().getId().equals(dbUser.getId())) {
                    problem = "That email address is already used by another account.";
                }
            }

            if (problem != null) {
                model.addAttribute("error", problem);
                model.addAttribute("user", dbUser);
                return "profile-edit";
            }

            dbUser.setFullName(name);
            dbUser.setEmail(mail);
            dbUser.setPhone(phoneNumber);
            dbUser.setAddress(addressValue.isEmpty() ? null : addressValue);

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
