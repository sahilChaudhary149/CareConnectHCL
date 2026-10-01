package com.careconnect.careconnect.controller;

import com.careconnect.careconnect.model.MedicalRecord;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import com.careconnect.careconnect.service.MedicalRecordService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final UserRepository userRepository;

    public MedicalRecordController(
            MedicalRecordService medicalRecordService,
            UserRepository userRepository) {
        this.medicalRecordService = medicalRecordService;
        this.userRepository = userRepository;
    }

    @GetMapping("/medical-records")
    public String medicalRecords(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        String dashboardUrl;
        List<MedicalRecord> records;
        boolean canAddRecord = false;

        if ("PATIENT".equals(user.getRole())) {
            dashboardUrl = "/patient-dashboard";
            records = medicalRecordService.getPatientRecords(user.getId());
        } else if ("DOCTOR".equals(user.getRole())) {
            dashboardUrl = "/doctor-dashboard";
            records = medicalRecordService.getAllRecords();
            canAddRecord = true;
        } else {
            dashboardUrl = "/admin-dashboard";
            records = medicalRecordService.getAllRecords();
            canAddRecord = true;
        }

        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        model.addAttribute("records", records);
        model.addAttribute("userMap", userMap);
        model.addAttribute("dashboardUrl", dashboardUrl);
        model.addAttribute("canAddRecord", canAddRecord);
        model.addAttribute("currentUser", user);

        return "medical-records";
    }

    @GetMapping("/add-medical-record")
    public String addMedicalRecordPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if (!"DOCTOR".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return "redirect:/patient-dashboard";
        }

        MedicalRecord record = new MedicalRecord();
        boolean isDoctor = "DOCTOR".equals(user.getRole());
        if (isDoctor) {
            record.setDoctorId(user.getId());
        }

        List<User> patients = userRepository.findByRole("PATIENT");
        List<User> doctors = userRepository.findByRole("DOCTOR");

        String dashboardUrl = isDoctor ? "/doctor-dashboard" : "/admin-dashboard";

        model.addAttribute("medicalRecord", record);
        model.addAttribute("patients", patients);
        model.addAttribute("doctors", doctors);
        model.addAttribute("isDoctor", isDoctor);
        model.addAttribute("doctorName", user.getName());
        model.addAttribute("dashboardUrl", dashboardUrl);

        return "add-medical-record";
    }

    @PostMapping("/add-medical-record")
    public String saveMedicalRecord(
            @ModelAttribute MedicalRecord medicalRecord,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/login";
        }

        if ("DOCTOR".equals(user.getRole())) {
            medicalRecord.setDoctorId(user.getId());
        }

        medicalRecordService.saveRecord(medicalRecord);

        return "redirect:/medical-records";
    }
}