package com.careconnect.careconnect;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class DoctorSpecialtyTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        cleanUp();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        userRepository.findByEmail("test.cardiologist@careconnect.test").ifPresent(userRepository::delete);
        userRepository.findByEmail("test.rheumatologist@careconnect.test").ifPresent(userRepository::delete);
        userRepository.findByEmail("test.dashboard.doc@careconnect.test").ifPresent(userRepository::delete);
    }

    @Test
    void testDoctorRegistrationWithSpeciality() throws Exception {
        String email = "test.cardiologist@careconnect.test";
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        mockMvc.perform(post("/register")
                        .param("name", "Dr. Cardiac Tester")
                        .param("email", email)
                        .param("phone", "+91 99999 11111")
                        .param("password", "doctor123")
                        .param("role", "DOCTOR")
                        .param("specialization", "Cardiology"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true&role=DOCTOR"));

        User doctor = userRepository.findByEmail(email).orElse(null);
        assertNotNull(doctor, "Doctor account should be saved in DB");
        assertEquals("DOCTOR", doctor.getRole());
        assertEquals("Cardiology", doctor.getSpecialization());
        assertEquals("AVAILABLE", doctor.getAvailabilityStatus());
        assertEquals("Mon - Fri", doctor.getAvailableDays());
        assertEquals("09:00 AM - 05:00 PM", doctor.getAvailableTime());
    }

    @Test
    void testDoctorRegistrationWithCustomSpeciality() throws Exception {
        String email = "test.rheumatologist@careconnect.test";
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        mockMvc.perform(post("/register")
                        .param("name", "Dr. Rheuma Tester")
                        .param("email", email)
                        .param("phone", "+91 99999 22222")
                        .param("password", "doctor123")
                        .param("role", "DOCTOR")
                        .param("specialization", "OTHER")
                        .param("customSpecialization", "Rheumatology"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true&role=DOCTOR"));

        User doctor = userRepository.findByEmail(email).orElse(null);
        assertNotNull(doctor, "Doctor account should be saved in DB");
        assertEquals("DOCTOR", doctor.getRole());
        assertEquals("Rheumatology", doctor.getSpecialization());
    }

    @Test
    void testDoctorDashboardDisplaysSpecialityAndAllowsUpdate() throws Exception {
        String email = "test.dashboard.doc@careconnect.test";
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        User doctor = new User("Dr. Roster Specialist", email, "docpass123", "DOCTOR");
        doctor.setSpecialization("Pediatrics");
        doctor.setAvailabilityStatus("AVAILABLE");
        doctor.setAvailableDays("Mon - Thu");
        doctor.setAvailableTime("10:00 AM - 04:00 PM");
        doctor = userRepository.save(doctor);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", doctor);

        // Verify GET doctor-dashboard contains specialization in model and rendered HTML
        mockMvc.perform(get("/doctor-dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor-dashboard"))
                .andExpect(model().attribute("specialization", "Pediatrics"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pediatrics")));

        // Update Doctor Speciality from dashboard modal
        mockMvc.perform(post("/doctor/update-specialty").session(session)
                        .param("specialization", "Neurology")
                        .param("availableDays", "Tue - Sat")
                        .param("availableTime", "08:30 AM - 02:30 PM")
                        .param("availabilityStatus", "AVAILABLE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor-dashboard?updated=true"));

        User updatedDoctor = userRepository.findById(doctor.getId()).orElse(null);
        assertNotNull(updatedDoctor);
        assertEquals("Neurology", updatedDoctor.getSpecialization());
        assertEquals("Tue - Sat", updatedDoctor.getAvailableDays());
        assertEquals("08:30 AM - 02:30 PM", updatedDoctor.getAvailableTime());
    }

    @Autowired
    private com.careconnect.careconnect.service.MedicalRecordService medicalRecordService;

    @Autowired
    private com.careconnect.careconnect.repository.MedicalRecordRepository medicalRecordRepository;

    @Test
    void testLatestMedicalReportAppearsFirstInList() throws Exception {
        com.careconnect.careconnect.model.MedicalRecord r1 = new com.careconnect.careconnect.model.MedicalRecord();
        r1.setPatientId(999L);
        r1.setDoctorId(888L);
        r1.setDiagnosis("Older Report");
        r1.setClinicalNotes("Older observation notes");
        r1 = medicalRecordRepository.save(r1);

        com.careconnect.careconnect.model.MedicalRecord r2 = new com.careconnect.careconnect.model.MedicalRecord();
        r2.setPatientId(999L);
        r2.setDoctorId(888L);
        r2.setDiagnosis("Latest Report");
        r2.setClinicalNotes("Latest observation notes");
        r2 = medicalRecordRepository.save(r2);

        java.util.List<com.careconnect.careconnect.model.MedicalRecord> all = medicalRecordService.getAllRecords();
        assertFalse(all.isEmpty());
        // Verify latest report (highest ID) appears at the top (index 0)
        assertEquals(r2.getId(), all.get(0).getId());

        java.util.List<com.careconnect.careconnect.model.MedicalRecord> patientRecords = medicalRecordService.getPatientRecords(999L);
        assertEquals(2, patientRecords.size());
        assertEquals(r2.getId(), patientRecords.get(0).getId());
        assertEquals(r1.getId(), patientRecords.get(1).getId());

        // Cleanup
        medicalRecordRepository.delete(r1);
        medicalRecordRepository.delete(r2);
    }

    @Test
    void testPatientDashboardDisplays20Entries() throws Exception {
        User patient = userRepository.findByEmail("patient@demo.com").orElse(null);
        assertNotNull(patient, "Demo patient should exist in database");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", patient);

        mockMvc.perform(get("/patient-dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("patient-dashboard"))
                .andExpect(model().attributeExists("appointments"))
                .andExpect(model().attributeExists("medicalRecords"))
                .andExpect(model().attributeExists("prescriptions"))
                .andExpect(model().attribute("totalAppts", 20))
                .andExpect(model().attribute("totalRecords", 20))
                .andExpect(model().attribute("totalPrescriptions", 20));
    }

    @Test
    void testDoctorDashboardDisplays20Entries() throws Exception {
        User doctor = userRepository.findByEmail("doctor@demo.com").orElse(null);
        assertNotNull(doctor, "Demo doctor should exist in database");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", doctor);

        mockMvc.perform(get("/doctor-dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor-dashboard"))
                .andExpect(model().attributeExists("appointments"))
                .andExpect(model().attributeExists("medicalRecords"))
                .andExpect(model().attributeExists("prescriptions"))
                .andExpect(model().attributeExists("patientMap"))
                .andExpect(model().attribute("totalAppts", 20))
                .andExpect(model().attribute("totalRecords", 20))
                .andExpect(model().attribute("totalPrescriptions", 20));
    }

    @Test
    void testDrNehaDashboardDisplays20Entries() throws Exception {
        User doctor = userRepository.findByEmail("neha123@gmail.com").orElse(null);
        assertNotNull(doctor, "Dr. Neha should exist in database");
        assertEquals("Dr. Neha", doctor.getName());
        assertEquals("Ophthalmology", doctor.getSpecialization());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", doctor);

        mockMvc.perform(get("/doctor-dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor-dashboard"))
                .andExpect(model().attribute("doctorName", "Dr. Neha"))
                .andExpect(model().attribute("specialization", "Ophthalmology"))
                .andExpect(model().attributeExists("appointments"))
                .andExpect(model().attributeExists("medicalRecords"))
                .andExpect(model().attributeExists("prescriptions"))
                .andExpect(model().attributeExists("patientMap"))
                .andExpect(model().attribute("totalAppts", 20))
                .andExpect(model().attribute("totalRecords", 20))
                .andExpect(model().attribute("totalPrescriptions", 20));
    }

    @Test
    void testAdminDoctorDirectoryRendering() throws Exception {
        User admin = userRepository.findByEmail("admin@careconnect.com").orElse(null);
        if (admin == null) {
            admin = new User("System Admin", "admin@careconnect.com", "admin123", "ADMIN");
            admin = userRepository.save(admin);
        }

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);

        mockMvc.perform(get("/admin/doctors").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("doctors"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attributeExists("totalDoctors"))
                .andExpect(model().attributeExists("availableCount"))
                .andExpect(model().attributeExists("specCount"));
    }

    @Test
    void testAdminPatientRegistryRendering() throws Exception {
        User admin = userRepository.findByEmail("admin@careconnect.com").orElse(null);
        if (admin == null) {
            admin = new User("System Admin", "admin@careconnect.com", "admin123", "ADMIN");
            admin = userRepository.save(admin);
        }

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);

        mockMvc.perform(get("/admin/patients").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("patients"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("totalPatients"))
                .andExpect(model().attributeExists("phoneCount"));
    }

    @Test
    void testAdminAvailabilityRendering() throws Exception {
        User admin = userRepository.findByEmail("admin@careconnect.com").orElse(null);
        if (admin == null) {
            admin = new User("System Admin", "admin@careconnect.com", "admin123", "ADMIN");
            admin = userRepository.save(admin);
        }

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);

        mockMvc.perform(get("/admin/availability").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor-availability"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attributeExists("totalDoctors"))
                .andExpect(model().attributeExists("availableCount"));
    }

    @Autowired
    private com.careconnect.careconnect.service.UserService userService;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void testBcryptPasswordEncryptionAndAuthentication() throws Exception {
        String testEmail = "test.bcrypt.user@careconnect.test";
        userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);

        // Register new user via registration endpoint
        mockMvc.perform(post("/register")
                        .param("name", "Bcrypt Security Tester")
                        .param("email", testEmail)
                        .param("phone", "+91 91234 56789")
                        .param("password", "SecretPass123!")
                        .param("role", "PATIENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true&role=PATIENT"));

        // Verify user in DB has BCrypt hashed password (not plain text)
        User savedUser = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(savedUser, "Registered user must exist in DB");
        assertNotEquals("SecretPass123!", savedUser.getPassword(), "Password must NOT be stored in plain text");
        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"),
                "Password must be formatted as a valid BCrypt hash ($2a$ or $2b$)");
        assertTrue(passwordEncoder.matches("SecretPass123!", savedUser.getPassword()),
                "BCrypt passwordEncoder should verify raw password against stored hash");

        // Verify successful login with correct password
        User loggedIn = userService.loginUser(testEmail, "SecretPass123!");
        assertNotNull(loggedIn, "User should be able to log in with correct password");
        assertEquals(savedUser.getId(), loggedIn.getId());

        // Verify failed login with incorrect password
        User failedLogin = userService.loginUser(testEmail, "WrongPassword999");
        assertNull(failedLogin, "Login with wrong password must return null");

        // Cleanup
        userRepository.delete(savedUser);
    }
}
