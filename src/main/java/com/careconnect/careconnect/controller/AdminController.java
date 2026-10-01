package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        model.addAttribute("doctorCount", userRepository.findByRole("DOCTOR").size());
        model.addAttribute("patientCount", userRepository.findByRole("PATIENT").size());

        return "admin-dashboard";
    }

    // Doctor Information & Directory
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
        long availableCount = doctors.stream()
                .filter(d -> "AVAILABLE".equalsIgnoreCase(d.getAvailabilityStatus()))
                .count();
        long specCount = doctors.stream()
                .map(User::getSpecialization)
                .filter(s -> s != null && !s.trim().isEmpty())
                .distinct()
                .count();

        model.addAttribute("adminName", user.getName());
        model.addAttribute("doctors", doctors);
        model.addAttribute("totalDoctors", doctors.size());
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("unavailableCount", doctors.size() - availableCount);
        model.addAttribute("specCount", specCount);

        return "doctors";
    }

    // Doctor Availability & Duty Schedule Management
    @GetMapping("/admin/availability")
    public String doctorAvailability(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"ADMIN".equals(user.getRole())) {
            return "redirect:/";
        }

        List<User> doctors = userRepository.findByRole("DOCTOR");

        long availableCount = doctors.stream()
                .filter(d -> "AVAILABLE".equalsIgnoreCase(d.getAvailabilityStatus()))
                .count();

        model.addAttribute("adminName", user.getName());
        model.addAttribute("doctors", doctors);
        model.addAttribute("totalDoctors", doctors.size());
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("unavailableCount", doctors.size() - availableCount);

        return "doctor-availability";
    }

    // Toggle Doctor Availability Status (One-click On Duty / Off Duty)
    @GetMapping("/admin/availability/toggle/{id}")
    public String toggleAvailability(@PathVariable Long id, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        User doctor = userRepository.findById(id).orElse(null);
        if (doctor != null) {
            if ("AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                doctor.setAvailabilityStatus("UNAVAILABLE");
            } else {
                doctor.setAvailabilityStatus("AVAILABLE");
            }
            userRepository.save(doctor);
        }

        return "redirect:/admin/availability";
    }

    // Update Doctor Working Days & Timings
    @PostMapping("/admin/availability/update")
    public String updateSchedule(
            @RequestParam Long doctorId,
            @RequestParam String availableDays,
            @RequestParam String availableTime,
            @RequestParam String availabilityStatus,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null || !"ADMIN".equals(user.getRole())) {
            return "redirect:/login";
        }

        User doctor = userRepository.findById(doctorId).orElse(null);
        if (doctor != null) {
            doctor.setAvailableDays(availableDays);
            doctor.setAvailableTime(availableTime);
            doctor.setAvailabilityStatus(availabilityStatus);
            userRepository.save(doctor);
        }

        return "redirect:/admin/availability?saved=true";
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
        long phoneCount = patients.stream()
                .filter(p -> p.getPhone() != null && !p.getPhone().trim().isEmpty())
                .count();

        model.addAttribute("adminName", user.getName());
        model.addAttribute("patients", patients);
        model.addAttribute("totalPatients", patients.size());
        model.addAttribute("phoneCount", phoneCount);

        return "patients";
    }
}