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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Lets a system administrator see every registered account and edit it.
 * The username is never changed because other records refer to it.
 */
@Controller
public class UserAdminController {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    private final UserRepository userRepository;

    @Autowired
    public UserAdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/admin/users")
    public String listUsers(@RequestParam(required = false) String q,
                            @RequestParam(required = false) String role,
                            HttpSession session, Model model) {
        User admin = currentAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Role roleFilter = parseRole(role);

        List<User> all = userRepository.findAll();
        List<User> users = all.stream()
                .filter(u -> roleFilter == null || u.getRole() == roleFilter)
                .filter(u -> query.isEmpty()
                        || contains(u.getUsername(), query)
                        || contains(u.getFullName(), query)
                        || contains(u.getEmail(), query)
                        || contains(u.getPhone(), query))
                .sorted(Comparator.comparing((User u) -> u.getRole() == null ? 99 : u.getRole().ordinal())
                        .thenComparing(u -> u.getFullName() == null ? "" : u.getFullName().toLowerCase(Locale.ROOT)))
                .toList();

        model.addAttribute("user", admin);
        model.addAttribute("users", users);
        model.addAttribute("totalUsers", all.size());
        model.addAttribute("roles", Role.values());
        model.addAttribute("q", q == null ? "" : q.trim());
        model.addAttribute("roleFilter", roleFilter == null ? "" : roleFilter.name());
        return "user-list";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model,
                               RedirectAttributes redirect) {
        User admin = currentAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Optional<User> target = userRepository.findById(id);
        if (target.isEmpty()) {
            redirect.addFlashAttribute("error", "That account no longer exists.");
            return "redirect:/admin/users";
        }
        fillEditModel(model, admin, target.get());
        return "user-edit";
    }

    @PostMapping("/admin/users/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam(required = false) String address,
                             @RequestParam(required = false) String role,
                             @RequestParam(required = false) String newPassword,
                             HttpSession session, Model model, RedirectAttributes redirect) {
        User admin = currentAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            redirect.addFlashAttribute("error", "That account no longer exists.");
            return "redirect:/admin/users";
        }
        User target = found.get();
        boolean editingSelf = target.getId().equals(admin.getId());

        String name = trim(fullName);
        String mail = trim(email);
        String phoneNo = trim(phone);
        String addr = trim(address);
        String password = newPassword == null ? "" : newPassword;
        Role newRole = editingSelf ? target.getRole() : parseRole(role);

        String problem = validate(target, name, mail, phoneNo, addr, newRole, password);

        if (problem != null) {
            // Show the form again with what the admin typed, never the stored password hash.
            User shown = new User();
            shown.setId(target.getId());
            shown.setUsername(target.getUsername());
            shown.setFullName(name);
            shown.setEmail(mail);
            shown.setPhone(phoneNo);
            shown.setAddress(addr);
            shown.setRole(newRole == null ? target.getRole() : newRole);
            fillEditModel(model, admin, shown);
            model.addAttribute("error", problem);
            return "user-edit";
        }

        target.setFullName(name);
        target.setEmail(mail);
        target.setPhone(phoneNo);
        target.setAddress(addr);
        target.setRole(newRole);
        if (!password.isEmpty()) {
            target.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        }
        User saved = userRepository.save(target);

        if (editingSelf) {
            session.setAttribute("user", saved);
        }

        redirect.addFlashAttribute("success", "Account '" + saved.getUsername() + "' was updated.");
        return "redirect:/admin/users";
    }

    private String validate(User target, String name, String mail, String phoneNo, String addr,
                            Role newRole, String password) {
        if (name == null) {
            return "Full name is required.";
        }
        if (name.length() > 100) {
            return "Full name cannot be longer than 100 characters.";
        }
        if (mail == null || !mail.matches(EMAIL_PATTERN)) {
            return "Please provide a valid email address.";
        }
        if (mail.length() > 100) {
            return "Email cannot be longer than 100 characters.";
        }
        if (phoneNo == null) {
            return "Phone number is required.";
        }
        if (phoneNo.length() > 20) {
            return "Phone number cannot be longer than 20 characters.";
        }
        if (addr != null && addr.length() > 255) {
            return "Address cannot be longer than 255 characters.";
        }
        if (newRole == null) {
            return "Please choose a role.";
        }
        Optional<User> sameEmail = userRepository.findByEmail(mail);
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(target.getId())) {
            return "That email address is already used by another account.";
        }
        if (!password.isEmpty() && password.length() < 6) {
            return "The new password must be at least 6 characters long.";
        }
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            return "This is the only system administrator, so its role cannot be changed.";
        }
        return null;
    }

    private void fillEditModel(Model model, User admin, User target) {
        model.addAttribute("user", admin);
        model.addAttribute("target", target);
        model.addAttribute("roles", Role.values());
        model.addAttribute("editingSelf", target.getId() != null && target.getId().equals(admin.getId()));
    }

    private User currentAdmin(HttpSession session) {
        Object attribute = session.getAttribute("user");
        if (attribute instanceof User u && u.getRole() == Role.ADMIN) {
            return u;
        }
        return null;
    }

    private Role parseRole(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
