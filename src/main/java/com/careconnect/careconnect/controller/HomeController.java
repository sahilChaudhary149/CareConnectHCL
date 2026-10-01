package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.dto.AppointmentDTO;
import com.careconnect.careconnect.model.Appointment;
import com.careconnect.careconnect.model.MedicalRecord;
import com.careconnect.careconnect.model.Prescription;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.AppointmentRepository;
import com.careconnect.careconnect.repository.MedicalRecordRepository;
import com.careconnect.careconnect.repository.PrescriptionRepository;
import com.careconnect.careconnect.repository.UserRepository;
import com.careconnect.careconnect.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final UserService userService;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;

    public HomeController(
            UserService userService,
            AppointmentRepository appointmentRepository,
            MedicalRecordRepository medicalRecordRepository,
            PrescriptionRepository prescriptionRepository,
            UserRepository userRepository) {
        this.userService = userService;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(required = false) String role, Model model) {
        if (!model.containsAttribute("user")) {
            User user = new User();
            if (role != null && !role.trim().isEmpty()) {
                user.setRole(role.trim().toUpperCase());
            }
            model.addAttribute("user", user);
        }
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute User user,
            @RequestParam(required = false) String customSpecialization,
            Model model) {
        String email = (user.getEmail() != null) ? user.getEmail().trim() : "";
        user.setEmail(email);

        if (user.getPhone() != null) {
            user.setPhone(user.getPhone().trim());
        }

        if (email.isEmpty()) {
            model.addAttribute("errorMessage", "Email address is required.");
            model.addAttribute("user", user);
            return "register";
        }

        if (userService.existsByEmail(email)) {
            model.addAttribute("isDuplicate", true);
            model.addAttribute("errorMessage", "An account with the email '" + email + "' is already registered. Please log in instead or use another email.");
            model.addAttribute("user", user);
            return "register";
        }

        try {
            if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
                String spec = user.getSpecialization();
                if ("OTHER".equalsIgnoreCase(spec) && customSpecialization != null && !customSpecialization.trim().isEmpty()) {
                    spec = customSpecialization.trim();
                } else if (spec != null) {
                    spec = spec.trim();
                }

                if (spec == null || spec.isEmpty() || "OTHER".equalsIgnoreCase(spec)) {
                    spec = "General Medicine";
                }
                user.setSpecialization(spec);

                if (user.getAvailabilityStatus() == null || user.getAvailabilityStatus().trim().isEmpty()) {
                    user.setAvailabilityStatus("AVAILABLE");
                }
                if (user.getAvailableDays() == null || user.getAvailableDays().trim().isEmpty()) {
                    user.setAvailableDays("Mon - Fri");
                }
                if (user.getAvailableTime() == null || user.getAvailableTime().trim().isEmpty()) {
                    user.setAvailableTime("09:00 AM - 05:00 PM");
                }
            } else {
                user.setSpecialization(null);
            }

            userService.registerUser(user);
            String roleParam = (user.getRole() != null && !user.getRole().isEmpty()) ? user.getRole() : "PATIENT";
            return "redirect:/login?registered=true&role=" + roleParam;
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("isDuplicate", true);
            model.addAttribute("errorMessage", "An account with the email '" + email + "' already exists. Please log in instead.");
            model.addAttribute("user", user);
            return "register";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Registration failed: " + ex.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
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
    public String patientDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login?role=PATIENT";
        }
        User freshUser = userService.findById(user.getId());
        if (freshUser != null) {
            user = freshUser;
            session.setAttribute("loggedInUser", user);
        }

        Long patientId = user.getId();

        // Build doctors lookup map
        Map<Long, User> doctorMap = userRepository.findByRole("DOCTOR").stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        final User finalUser = user;
        List<Appointment> rawAppointments = appointmentRepository.findByPatientId(patientId);
        List<AppointmentDTO> appointments = rawAppointments.stream().map(a -> {
            User doc = doctorMap.get(a.getDoctorId());
            return new AppointmentDTO(
                    a.getId(),
                    patientId,
                    finalUser.getName(),
                    finalUser.getEmail(),
                    finalUser.getPhone(),
                    a.getDoctorId(),
                    doc != null ? doc.getName() : "Attending Doctor",
                    doc != null && doc.getSpecialization() != null ? doc.getSpecialization() : "General",
                    doc != null ? doc.getEmail() : "",
                    a.getAppointmentDate(),
                    a.getReason(),
                    a.getStatus()
            );
        }).toList();

        List<MedicalRecord> medicalRecords = medicalRecordRepository.findByPatientIdOrderByIdDesc(patientId);
        List<Prescription> prescriptions = prescriptionRepository.findByPatientId(patientId);

        model.addAttribute("patientName", user.getName());
        model.addAttribute("currentUser", user);
        model.addAttribute("appointments", appointments);
        model.addAttribute("medicalRecords", medicalRecords);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("doctorMap", doctorMap);

        model.addAttribute("totalAppts", appointments.size());
        model.addAttribute("totalRecords", medicalRecords.size());
        model.addAttribute("totalPrescriptions", prescriptions.size());

        return "patient-dashboard";
    }

    @GetMapping("/doctor-dashboard")
    public String doctorDashboard(
            @RequestParam(required = false) Boolean updated,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login?role=DOCTOR";
        }
        User freshUser = userService.findById(user.getId());
        if (freshUser != null) {
            user = freshUser;
            session.setAttribute("loggedInUser", user);
        }
        model.addAttribute("doctorName", user.getName());
        model.addAttribute("specialization", user.getSpecialization());
        model.addAttribute("availabilityStatus", user.getAvailabilityStatus());
        model.addAttribute("availableDays", user.getAvailableDays());
        model.addAttribute("availableTime", user.getAvailableTime());
        model.addAttribute("currentUser", user);

        Long doctorId = user.getId();

        // Build patients lookup map
        Map<Long, User> patientMap = userRepository.findByRole("PATIENT").stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        final User finalDoctor = user;
        List<Appointment> doctorAppts = appointmentRepository.findByDoctorId(doctorId);
        List<AppointmentDTO> appointments = doctorAppts.stream().map(a -> {
            User patient = patientMap.get(a.getPatientId());
            return new AppointmentDTO(
                    a.getId(),
                    a.getPatientId(),
                    patient != null ? patient.getName() : "Patient #" + a.getPatientId(),
                    patient != null ? patient.getEmail() : "",
                    patient != null ? patient.getPhone() : "",
                    doctorId,
                    finalDoctor.getName(),
                    finalDoctor.getSpecialization() != null ? finalDoctor.getSpecialization() : "General",
                    finalDoctor.getEmail(),
                    a.getAppointmentDate(),
                    a.getReason(),
                    a.getStatus()
            );
        }).toList();

        List<MedicalRecord> medicalRecords = medicalRecordRepository.findByDoctorIdOrderByIdDesc(doctorId);
        List<Prescription> prescriptions = prescriptionRepository.findByDoctorId(doctorId);

        int totalAppts = doctorAppts.size();
        long pendingAppts = doctorAppts.stream().filter(a -> "PENDING".equalsIgnoreCase(a.getStatus())).count();
        long confirmedAppts = doctorAppts.stream().filter(a -> "CONFIRMED".equalsIgnoreCase(a.getStatus())).count();

        model.addAttribute("appointments", appointments);
        model.addAttribute("medicalRecords", medicalRecords);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("patientMap", patientMap);

        model.addAttribute("totalAppts", totalAppts);
        model.addAttribute("pendingAppts", pendingAppts);
        model.addAttribute("confirmedAppts", confirmedAppts);
        model.addAttribute("totalRecords", medicalRecords.size());
        model.addAttribute("totalPrescriptions", prescriptions.size());

        if (Boolean.TRUE.equals(updated)) {
            model.addAttribute("successMessage", "Doctor speciality and duty schedule updated successfully!");
        }
        return "doctor-dashboard";
    }

    @GetMapping("/doctor/toggle-duty")
    public String toggleDoctorDuty(HttpSession session) {
        User sessionUser = (User) session.getAttribute("loggedInUser");
        if (sessionUser == null || !"DOCTOR".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/login?role=DOCTOR";
        }
        User doctor = userService.findById(sessionUser.getId());
        if (doctor != null) {
            if ("AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
                doctor.setAvailabilityStatus("UNAVAILABLE");
            } else {
                doctor.setAvailabilityStatus("AVAILABLE");
            }
            userService.saveUser(doctor);
            session.setAttribute("loggedInUser", doctor);
        }
        return "redirect:/doctor-dashboard";
    }

    @PostMapping("/doctor/update-specialty")
    public String updateDoctorSpecialty(
            @RequestParam String specialization,
            @RequestParam(required = false) String customSpecialization,
            @RequestParam(required = false) String availableDays,
            @RequestParam(required = false) String availableTime,
            @RequestParam(required = false) String availabilityStatus,
            HttpSession session) {

        User sessionUser = (User) session.getAttribute("loggedInUser");
        if (sessionUser == null || !"DOCTOR".equalsIgnoreCase(sessionUser.getRole())) {
            return "redirect:/login?role=DOCTOR";
        }

        User doctor = userService.findById(sessionUser.getId());
        if (doctor != null) {
            String finalSpecialization = specialization;
            if ("OTHER".equalsIgnoreCase(specialization) && customSpecialization != null && !customSpecialization.trim().isEmpty()) {
                finalSpecialization = customSpecialization.trim();
            } else if (finalSpecialization != null) {
                finalSpecialization = finalSpecialization.trim();
            }

            if (finalSpecialization != null && !finalSpecialization.isEmpty() && !"OTHER".equalsIgnoreCase(finalSpecialization)) {
                doctor.setSpecialization(finalSpecialization);
            }
            if (availableDays != null && !availableDays.trim().isEmpty()) {
                doctor.setAvailableDays(availableDays.trim());
            }
            if (availableTime != null && !availableTime.trim().isEmpty()) {
                doctor.setAvailableTime(availableTime.trim());
            }
            if (availabilityStatus != null && !availabilityStatus.trim().isEmpty()) {
                doctor.setAvailabilityStatus(availabilityStatus.trim());
            }

            userService.saveUser(doctor);
            session.setAttribute("loggedInUser", doctor);
        }

        return "redirect:/doctor-dashboard?updated=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}