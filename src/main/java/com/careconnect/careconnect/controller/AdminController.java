package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Admin Dashboard
    @GetMapping("/admin-dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        model.addAttribute("adminName", user.getName());

        return "admin-dashboard";
    }

    // Doctor Information
    @GetMapping("/admin/doctors")
    public String doctorInformation(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        List<User> doctors = userRepository.findByRole("DOCTOR");

        model.addAttribute("doctors", doctors);

        return "doctors";
    }
    // Patient Information
    @GetMapping("/admin/patients")
    public String patientInformation(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        List<User> patients = userRepository.findByRole("PATIENT");

        model.addAttribute("patients", patients);

        return "patients";
    }
}