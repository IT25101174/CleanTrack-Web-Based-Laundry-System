package com.cleantrack.laundry_system.controller;

import com.cleantrack.laundry_system.model.Role;
import com.cleantrack.laundry_system.model.User;
import com.cleantrack.laundry_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
                        HttpServletRequest request,
                        HttpSession session,
                        Model model) {
        Optional<User> userOpt = userRepository.findByUsername(username == null ? "" : username.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Validate password using BCrypt
            if (BCrypt.checkpw(password, user.getPassword())) {
                // A fresh session id after sign in prevents session fixation
                request.changeSessionId();
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
        // Never trust an id sent from the form: it could overwrite an existing account
        user.setId(null);
        trimUserFields(user);

        String problem = validateNewUser(user, true);
        if (problem != null) {
            return registerError(user, model, problem, "register");
        }

        // Secure password with BCrypt
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);

        return "redirect:/login?registered=true";
    }

    // Common checks for the customer, employee and first-admin forms. Returns the first problem or null.
    private String validateNewUser(User user, boolean phoneRequired) {
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return "Username is required.";
        }
        if (user.getUsername().length() > 50) {
            return "Username cannot be longer than 50 characters.";
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            return "Password must be at least 6 characters long.";
        }
        if (user.getEmail() == null || !user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return "Please provide a valid email address.";
        }
        if (user.getFullName() == null || user.getFullName().isEmpty()) {
            return "Full name is required.";
        }
        if (phoneRequired && (user.getPhone() == null || user.getPhone().isEmpty())) {
            return "Phone number is required.";
        }
        if (user.getPhone() != null && user.getPhone().length() > 20) {
            return "Phone number cannot be longer than 20 characters.";
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return "Username already exists.";
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "Email is already registered.";
        }
        return null;
    }

    private void trimUserFields(User user) {
        user.setUsername(trimOrNull(user.getUsername()));
        user.setEmail(trimOrNull(user.getEmail()));
        user.setFullName(trimOrNull(user.getFullName()));
        user.setPhone(trimOrNull(user.getPhone()));
        user.setAddress(trimOrNull(user.getAddress()));
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // Shows the form again with the message. The password is never sent back to the browser.
    private String registerError(User user, Model model, String message, String view) {
        user.setPassword(null);
        model.addAttribute("error", message);
        return view;
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

        user.setId(null);
        trimUserFields(user);

        if (user.getRole() == null || user.getRole() == Role.CUSTOMER) {
            return registerError(user, model, "Please choose an employee role.", "admin-create-user");
        }
        String problem = validateNewUser(user, true);
        if (problem != null) {
            return registerError(user, model, problem, "admin-create-user");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
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
        
        if (user.getRole() != null && "CUSTOMER".equals(user.getRole().name())) {
            return "redirect:/customer-dashboard";
        }
        
        return "dashboard";
    }
    
    // First-time setup page. It only works while the system has no administrator,
    // so it cannot be used later to create more admin accounts without signing in.
    @GetMapping("/secret-admin-setup-99")
    public String showSecretAdminRegistrationForm(Model model) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return "redirect:/login";
        }
        model.addAttribute("user", new User());
        return "secret-admin-register";
    }

    @PostMapping("/secret-admin-setup-99")
    public String secretRegisterAdmin(@ModelAttribute User user, Model model) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return "redirect:/login";
        }

        user.setId(null);
        trimUserFields(user);

        String problem = validateNewUser(user, true);
        if (problem != null) {
            return registerError(user, model, problem, "secret-admin-register");
        }

        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setRole(Role.ADMIN);
        userRepository.save(user);

        return "redirect:/login?registered=true";
    }
}
