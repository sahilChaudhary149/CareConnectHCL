package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute User user) {
        userService.registerUser(user);
        String roleParam = (user.getRole() != null && !user.getRole().isEmpty()) ? user.getRole() : "PATIENT";
        return "redirect:/login?registered=true&role=" + roleParam;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String role,
            HttpSession session) {

        User user = userService.loginUser(email, password);

        if (user != null) {

            // Validate against selected dashboard role if specified
            if (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase(user.getRole())) {
                return "redirect:/login?error=role_mismatch&expectedRole=" + role + "&actualRole=" + user.getRole();
            }

            // Store logged-in user
            session.setAttribute("loggedInUser", user);

            if ("PATIENT".equals(user.getRole())) {
                return "redirect:/patient-dashboard";
            }

            if ("DOCTOR".equals(user.getRole())) {
                return "redirect:/doctor-dashboard";
            }
            if ("ADMIN".equals(user.getRole())) {
                return "redirect:/admin-dashboard";
            }
        }

        String roleQuery = (role != null && !role.trim().isEmpty()) ? "&role=" + role : "";
        return "redirect:/login?error=invalid" + roleQuery;
    }

    @GetMapping("/patient-dashboard")
    public String patientDashboard() {
        return "patient-dashboard";
    }

    @GetMapping("/doctor-dashboard")
    public String doctorDashboard() {
        return "doctor-dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}