package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.dto.AppointmentDTO;
import com.careconnect.careconnect.model.Appointment;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import com.careconnect.careconnect.service.AppointmentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
    }

    // Show appointment booking page
    @GetMapping("/book-appointment")
    public String bookAppointmentPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login?role=PATIENT";
        }

        if (!"PATIENT".equals(user.getRole())) {
            return "redirect:/doctor-dashboard";
        }

        Appointment appointment = new Appointment();
        appointment.setPatientId(user.getId());

        List<User> doctors = userRepository.findByRole("DOCTOR");

        model.addAttribute("appointment", appointment);
        model.addAttribute("doctors", doctors);
        model.addAttribute("patientName", user.getName());
        model.addAttribute("patientId", user.getId());

        return "book-appointment";
    }

    // Save appointment
    @PostMapping("/book-appointment")
    public String bookAppointment(
            @ModelAttribute Appointment appointment,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        // Always lock to logged-in patient's ID
        appointment.setPatientId(user.getId());

        if (appointment.getStatus() == null || appointment.getStatus().isEmpty()) {
            appointment.setStatus("PENDING");
        }

        appointmentService.saveAppointment(appointment);

        return "redirect:/appointments";
    }

    // Show all appointments with names and role-based filtering
    @GetMapping("/appointments")
    public String appointments(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        List<Appointment> rawAppointments;
        boolean isDoctor = "DOCTOR".equals(user.getRole());
        boolean isAdmin = "ADMIN".equals(user.getRole());
        boolean isPatient = "PATIENT".equals(user.getRole());

        if (isDoctor) {
            rawAppointments = appointmentService.getDoctorAppointments(user.getId());
        } else if (isAdmin) {
            rawAppointments = appointmentService.getAllAppointments();
        } else {
            rawAppointments = appointmentService.getPatientAppointments(user.getId());
        }

        // Build lookup map for users to show names instead of raw IDs
        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<AppointmentDTO> appointmentDTOs = rawAppointments.stream().map(a -> {
            User patient = userMap.get(a.getPatientId());
            User doctor = userMap.get(a.getDoctorId());

            return new AppointmentDTO(
                    a.getId(),
                    a.getPatientId(),
                    patient != null ? patient.getName() : "Patient #" + a.getPatientId(),
                    patient != null ? patient.getEmail() : "",
                    patient != null ? patient.getPhone() : "",
                    a.getDoctorId(),
                    doctor != null ? doctor.getName() : "Attending Doctor",
                    doctor != null && doctor.getSpecialization() != null ? doctor.getSpecialization() : "General",
                    doctor != null ? doctor.getEmail() : "",
                    a.getAppointmentDate(),
                    a.getReason(),
                    a.getStatus()
            );
        }).toList();

        model.addAttribute("appointments", appointmentDTOs);
        model.addAttribute("isDoctor", isDoctor);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isPatient", isPatient);
        model.addAttribute("currentUser", user);

        return "appointments";
    }

    // Confirm appointment (Protected - Only assigned Doctor or Admin)
    @GetMapping("/appointment/confirm/{id}")
    public String confirmAppointment(@PathVariable Long id, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCTOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return "redirect:/appointments";
        }

        appointmentService.updateStatus(id, "CONFIRMED");

        return "redirect:/appointments";
    }

    // Reject appointment (Protected - Only assigned Doctor or Admin)
    @GetMapping("/appointment/reject/{id}")
    public String rejectAppointment(@PathVariable Long id, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCTOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return "redirect:/appointments";
        }

        appointmentService.updateStatus(id, "REJECTED");

        return "redirect:/appointments";
    }
}