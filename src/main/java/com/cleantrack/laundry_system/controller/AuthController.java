package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.Role;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Validate password using BCrypt
            if (BCrypt.checkpw(password, user.getPassword())) {
                session.setAttribute("user", user);
                session.setAttribute("role", user.getRole().name()); // Store role name in session if needed
                return "redirect:/dashboard";
            }
        }
        model.addAttribute("error", "Invalid username or password");
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(HttpSession session, Model model) {
        if (session.getAttribute("user") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute User user, Model model) {
        // Basic inline validations
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            model.addAttribute("error", "Username is required.");
            return "register";
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long.");
            return "register";
        }
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            model.addAttribute("error", "Please provide a valid email address.");
            return "register";
        }
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            model.addAttribute("error", "Phone number is required.");
            return "register";
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            model.addAttribute("error", "Full name is required.");
            return "register";
        }
        

        // Check for existing username
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "register";
        }

        // Check for existing email
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email is already registered.");
            return "register";
        }

        // Secure password with BCrypt
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setRole(Role.CUSTOMER);
        
        userRepository.save(user);

        return "redirect:/login?registered=true";
    }


    @GetMapping("/admin/users/new")
    public String showAdminRegistrationForm(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole() != Role.ADMIN) {
            return "redirect:/login";
        }
        model.addAttribute("user", new User());
        return "admin-create-user";
    }

    @PostMapping("/admin/users/new")
    public String adminRegisterUser(@ModelAttribute User user, HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null || loggedInUser.getRole() != Role.ADMIN) {
            return "redirect:/login";
        }

        // Basic validations
        if (user.getUsername() == null || user.getUsername().trim().isEmpty() ||
            user.getPassword() == null || user.getPassword().length() < 6 ||
            user.getEmail() == null || !user.getEmail().contains("@") ||
            user.getFullName() == null || user.getFullName().trim().isEmpty() ||
            user.getRole() == null) {
            model.addAttribute("error", "Please fill all required fields correctly.");
            return "admin-create-user";
        }

        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "Username already exists.");
            return "admin-create-user";
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            model.addAttribute("error", "Email is already registered.");
            return "admin-create-user";
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return "redirect:/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("user", user);
        return "dashboard";
    }
}
