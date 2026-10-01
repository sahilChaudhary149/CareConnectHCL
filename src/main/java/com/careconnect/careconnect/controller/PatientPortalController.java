package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.dto.AppointmentDTO;
import com.careconnect.careconnect.model.Appointment;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import com.careconnect.careconnect.service.AppointmentService;
import com.careconnect.careconnect.service.MedicalRecordService;
import com.careconnect.careconnect.service.PrescriptionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PatientPortalController {

    private final AppointmentService appointmentService;
    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    public PatientPortalController(
            AppointmentService appointmentService,
            MedicalRecordService medicalRecordService,
            PrescriptionService prescriptionService,
            UserRepository userRepository) {

        this.appointmentService = appointmentService;
        this.medicalRecordService = medicalRecordService;
        this.prescriptionService = prescriptionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/patient-portal")
    public String patientPortal(
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        // Check if user is logged in
        if (user == null) {
            return "redirect:/login";
        }

        // Allow only patients
        if (!"PATIENT".equals(user.getRole())) {
            return "redirect:/doctor-dashboard";
        }

        Long patientId = user.getId();

        // Build doctors lookup map to show doctor names
        Map<Long, User> doctorMap = userRepository.findByRole("DOCTOR").stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<Appointment> rawAppointments = appointmentService.getPatientAppointments(patientId);
        List<AppointmentDTO> appointmentDTOs = rawAppointments.stream().map(a -> {
            User doc = doctorMap.get(a.getDoctorId());
            return new AppointmentDTO(
                    a.getId(),
                    patientId,
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    a.getDoctorId(),
                    doc != null ? doc.getName() : "Attending Doctor",
                    doc != null && doc.getSpecialization() != null ? doc.getSpecialization() : "General",
                    doc != null ? doc.getEmail() : "",
                    a.getAppointmentDate(),
                    a.getReason(),
                    a.getStatus()
            );
        }).toList();

        model.addAttribute("appointments", appointmentDTOs);
        model.addAttribute("medicalRecords", medicalRecordService.getPatientRecords(patientId));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patientId));
        model.addAttribute("doctorMap", doctorMap);
        model.addAttribute("patientId", patientId);
        model.addAttribute("patientName", user.getName());

        return "patient-portal";
    }
}