package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.model.Prescription;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import com.careconnect.careconnect.service.PrescriptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    public PrescriptionController(PrescriptionService prescriptionService, UserRepository userRepository) {
        this.prescriptionService = prescriptionService;
        this.userRepository = userRepository;
    }

    // Show prescription form
    @GetMapping("/add-prescription")
    public String addPrescriptionPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCTOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return "redirect:/patient-dashboard";
        }

        Prescription prescription = new Prescription();
        prescription.setDoctorId(user.getId());

        List<User> patients = userRepository.findByRole("PATIENT");
        String dashboardUrl = "DOCTOR".equals(user.getRole()) ? "/doctor-dashboard" : "/admin-dashboard";

        model.addAttribute("prescription", prescription);
        model.addAttribute("patients", patients);
        model.addAttribute("doctorName", user.getName());
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "add-prescription";
    }

    // Save prescription
    @PostMapping("/add-prescription")
    public String savePrescription(
            @ModelAttribute Prescription prescription,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if ("DOCTOR".equals(user.getRole())) {
            prescription.setDoctorId(user.getId());
        }

        prescriptionService.savePrescription(prescription);

        return "redirect:/prescriptions";
    }

    // Show prescriptions
    @GetMapping("/prescriptions")
    public String prescriptions(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        String dashboardUrl;
        List<Prescription> prescriptions;
        boolean canAddPrescription = false;

        if ("PATIENT".equals(user.getRole())) {
            dashboardUrl = "/patient-dashboard";
            prescriptions = prescriptionService.getPatientPrescriptions(user.getId());
        } else if ("DOCTOR".equals(user.getRole())) {
            dashboardUrl = "/doctor-dashboard";
            prescriptions = prescriptionService.getDoctorPrescriptions(user.getId());
            canAddPrescription = true;
        } else {
            dashboardUrl = "/admin-dashboard";
            prescriptions = prescriptionService.getAllPrescriptions();
            canAddPrescription = true;
        }

        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("userMap", userMap);
        model.addAttribute("dashboardUrl", dashboardUrl);
        model.addAttribute("canAddPrescription", canAddPrescription);
        model.addAttribute("currentUser", user);

        return "prescriptions";
    }
}