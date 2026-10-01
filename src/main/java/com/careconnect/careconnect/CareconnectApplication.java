package com.careconnect.careconnect;

import com.careconnect.careconnect.model.Appointment;
import com.careconnect.careconnect.model.MedicalRecord;
import com.careconnect.careconnect.model.Prescription;
import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.AppointmentRepository;
import com.careconnect.careconnect.repository.MedicalRecordRepository;
import com.careconnect.careconnect.repository.PrescriptionRepository;
import com.careconnect.careconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class CareconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareconnectApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CommandLineRunner initDemoUsers(
			UserRepository userRepository,
			AppointmentRepository appointmentRepository,
			MedicalRecordRepository medicalRecordRepository,
			PrescriptionRepository prescriptionRepository,
			PasswordEncoder passwordEncoder,
			org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
		return args -> {

			// Load full real database entries (34 users, 93 appts, 84 records, 85 rx) if fresh database
			if (userRepository.count() < 30) {
				try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(
						new org.springframework.core.io.ClassPathResource("data_full.sql").getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
					String line;
					int loadedCount = 0;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.startsWith("INSERT INTO")) {
							try {
								jdbcTemplate.execute(line);
								loadedCount++;
							} catch (Exception ex) {
								// continue on duplicate key or syntax variation
							}
						}
					}
					System.out.println(">>> CareConnect Full Database Seeder: Successfully loaded " + loadedCount + " records from data_full.sql! Total users: " + userRepository.count());
				} catch (Exception e) {
					System.err.println(">>> Error loading data_full.sql: " + e.getMessage());
				}
			}

			// 0. Auto-upgrade any existing legacy plaintext passwords to secure BCrypt hashes
			List<User> allDbUsers = userRepository.findAll();
			for (User u : allDbUsers) {
				String pwd = u.getPassword();
				if (pwd != null && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
					u.setPassword(passwordEncoder.encode(pwd));
					userRepository.save(u);
				}
			}

			// 1. Admin Account
			if (userRepository.findByEmail("admin@careconnect.com").isEmpty()) {
				User admin = new User("System Admin", "admin@careconnect.com", passwordEncoder.encode("admin123"), "ADMIN");
				userRepository.save(admin);
			}

			// 2. Medical Specialist Doctors (10 Doctors)
			List<User> doctors = new ArrayList<>();
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Sarah Smith", "doctor@demo.com", "doctor123", "Cardiology", "+91 98765 01001", "Mon - Fri", "09:00 AM - 05:00 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Rajesh Kumar", "rajesh.kumar@careconnect.com", "doctor123", "Neurology", "+91 98765 01002", "Mon - Sat", "10:00 AM - 04:00 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Priya Sharma", "priya.sharma@careconnect.com", "doctor123", "Pediatrics", "+91 98765 01003", "Tue - Sat", "09:30 AM - 03:30 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Amit Verma", "amit.verma@careconnect.com", "doctor123", "Orthopedics", "+91 98765 01004", "Mon - Fri", "11:00 AM - 06:00 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Ananya Roy", "ananya.roy@careconnect.com", "doctor123", "Dermatology", "+91 98765 01005", "Wed - Sun", "10:00 AM - 05:00 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. David Miller", "david.miller@careconnect.com", "doctor123", "General Medicine", "+91 98765 01006", "Mon - Fri", "08:30 AM - 04:30 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Vikram Patel", "vikram.patel@careconnect.com", "doctor123", "Oncology", "+91 98765 01007", "Mon - Thu", "09:00 AM - 03:00 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Sunita Rao", "sunita.rao@careconnect.com", "doctor123", "Gynecology", "+91 98765 01008", "Tue - Sat", "10:00 AM - 04:30 PM", "AVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Robert Chen", "robert.chen@careconnect.com", "doctor123", "Ophthalmology", "+91 98765 01009", "Mon - Fri", "09:00 AM - 05:00 PM", "UNAVAILABLE"));
			doctors.add(getOrCreateDoctor(userRepository, passwordEncoder, "Dr. Neha Gupta", "neha.gupta@careconnect.com", "doctor123", "Psychiatry", "+91 98765 01010", "Mon - Sat", "12:00 PM - 07:00 PM", "AVAILABLE"));

			// 3. Registered Patients (12 Patients)
			List<User> patients = new ArrayList<>();
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "John Doe", "patient@demo.com", "patient123", "+91 98112 00001"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Gaurav Dwivedi", "gaurav.dwivedi@example.com", "patient123", "+91 98112 00002"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Rahul Mehra", "rahul.mehra@example.com", "patient123", "+91 98223 00003"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Sneha Kapoor", "sneha.kapoor@example.com", "patient123", "+91 98334 00004"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Rohan Sharma", "rohan.sharma@example.com", "patient123", "+91 98445 00005"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Pooja Patel", "pooja.patel@example.com", "patient123", "+91 98556 00006"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Aman Gupta", "aman.gupta@example.com", "patient123", "+91 98667 00007"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Ritu Verma", "ritu.verma@example.com", "patient123", "+91 98778 00008"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Deepak Joshi", "deepak.joshi@example.com", "patient123", "+91 98889 00009"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Meera Nair", "meera.nair@example.com", "patient123", "+91 98990 00010"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Karan Malhotra", "karan.m@example.com", "patient123", "+91 98101 00011"));
			patients.add(getOrCreatePatient(userRepository, passwordEncoder, "Divya Aggarwal", "divya.a@example.com", "patient123", "+91 98212 00012"));

			// 4. Clinical Appointments (Seed if less than 15 total)
			if (appointmentRepository.count() < 15 && !doctors.isEmpty() && !patients.isEmpty()) {
				createAppointment(appointmentRepository, patients.get(0).getId(), doctors.get(0).getId(), "2026-10-02T10:30", "Annual cardiac evaluation and ECG review", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(1).getId(), doctors.get(1).getId(), "2026-10-03T11:00", "Persistent migraine and tension headaches", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(2).getId(), doctors.get(2).getId(), "2026-10-04T09:30", "Routine pediatric immunization and growth check", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(3).getId(), doctors.get(3).getId(), "2026-10-05T14:00", "Follow-up for right knee joint pain and swelling", "PENDING");
				createAppointment(appointmentRepository, patients.get(4).getId(), doctors.get(4).getId(), "2026-10-06T11:30", "Skin allergy patch test and consultation", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(5).getId(), doctors.get(5).getId(), "2026-10-07T10:00", "Seasonal flu symptoms, fever, and persistent cough", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(6).getId(), doctors.get(6).getId(), "2026-10-08T15:30", "Post-operative oncology wellness checkup", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(7).getId(), doctors.get(7).getId(), "2026-10-09T10:30", "Prenatal routine ultrasound and blood panel", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(8).getId(), doctors.get(8).getId(), "2026-10-10T12:00", "Visual acuity examination and dry eye assessment", "PENDING");
				createAppointment(appointmentRepository, patients.get(9).getId(), doctors.get(9).getId(), "2026-10-11T16:00", "Anxiety management and sleep therapy consultation", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(10).getId(), doctors.get(0).getId(), "2026-10-12T09:00", "Palpitations during exercise and treadmill stress test", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(11).getId(), doctors.get(5).getId(), "2026-10-13T10:30", "General annual comprehensive physical examination", "PENDING");
				createAppointment(appointmentRepository, patients.get(1).getId(), doctors.get(3).getId(), "2026-10-14T11:00", "Lower back lumbar discomfort after long sitting", "CONFIRMED");
				createAppointment(appointmentRepository, patients.get(2).getId(), doctors.get(4).getId(), "2026-10-15T15:00", "Eczema flare-up review and topical refill", "REJECTED");
			}

			// 5. Medical Records (Seed if less than 12 total)
			if (medicalRecordRepository.count() < 12 && !doctors.isEmpty() && !patients.isEmpty()) {
				createRecord(medicalRecordRepository, patients.get(0).getId(), doctors.get(0).getId(),
						"Stage 1 Essential Hypertension",
						"BP recorded at 142/90 mmHg. Patient advised low-sodium DASH diet regimen and started on Amlodipine 5mg OD. Recheck scheduled in 4 weeks.");

				createRecord(medicalRecordRepository, patients.get(1).getId(), doctors.get(1).getId(),
						"Chronic Migraine without Aura",
						"Patient reports unilateral throbbing headache attacks 3-4 times per month. Trigger identified as sleep irregularity. Advised sleep hygiene protocol and Sumatriptan for acute episodes.");

				createRecord(medicalRecordRepository, patients.get(2).getId(), doctors.get(2).getId(),
						"Childhood Asthma & Bronchospasm",
						"Mild wheezing observed on deep expiration. Oxygen saturation 98% on room air. Prescribed Salbutamol inhaler PRN with pediatric spacer.");

				createRecord(medicalRecordRepository, patients.get(3).getId(), doctors.get(3).getId(),
						"Acute Right Knee Ligament Sprain",
						"Mild tenderness over medial collateral ligament after sports activity. Lachman test negative. Prescribed RICE protocol, knee brace, and NSAIDs for 7 days.");

				createRecord(medicalRecordRepository, patients.get(4).getId(), doctors.get(4).getId(),
						"Atopic Dermatitis (Eczema)",
						"Erythematous pruritic lesions on bilateral antecubital fossae. Emollient barrier cream prescribed with topical hydrocortisone 1% for flares. Avoid harsh soaps.");

				createRecord(medicalRecordRepository, patients.get(5).getId(), doctors.get(5).getId(),
						"Type 2 Diabetes Mellitus",
						"Fasting blood sugar 148 mg/dL, HbA1c 7.2%. Prescribed Metformin 500mg BID with meals. Dietary consultation requested. Follow-up HbA1c in 3 months.");

				createRecord(medicalRecordRepository, patients.get(6).getId(), doctors.get(6).getId(),
						"Post-Resection Surveillance - Clear",
						"Routine 6-month CT imaging reveals no evidence of recurrence or distant metastasis. Blood tumor markers within normal limits. Return in 6 months.");

				createRecord(medicalRecordRepository, patients.get(7).getId(), doctors.get(7).getId(),
						"First Trimester Normal Pregnancy",
						"Gestational age 10 weeks 3 days by ultrasound. Fetal heart rate 158 bpm. Normal development. Initiated prenatal multivitamins and folic acid.");

				createRecord(medicalRecordRepository, patients.get(8).getId(), doctors.get(8).getId(),
						"Bilateral Dry Eye Syndrome & Astigmatism",
						"Schirmer test positive for tear deficiency. Prescribed preservative-free lubricating carboxymethylcellulose eye drops QID. Updated prescription glasses issued.");

				createRecord(medicalRecordRepository, patients.get(9).getId(), doctors.get(9).getId(),
						"Generalized Anxiety Disorder (GAD)",
						"Patient presents with persistent worry, muscle tension, and initial insomnia for >6 months. Recommended CBT sessions and short-term Escitalopram 10mg.");
			}

			// 6. Prescriptions (Seed if less than 12 total)
			if (prescriptionRepository.count() < 12 && !doctors.isEmpty() && !patients.isEmpty()) {
				createRx(prescriptionRepository, patients.get(0).getId(), doctors.get(0).getId(),
						"Amlodipine Besylate", "5 mg - Once daily in morning", "30 Days", "Take with or without food. Monitor blood pressure weekly.");

				createRx(prescriptionRepository, patients.get(1).getId(), doctors.get(1).getId(),
						"Sumatriptan Succinate", "50 mg - Take at onset of migraine", "10 Tablets", "Do not take more than 200mg in 24 hours. Rest in dark quiet room.");

				createRx(prescriptionRepository, patients.get(2).getId(), doctors.get(2).getId(),
						"Salbutamol Inhaler (Ventolin)", "100 mcg - 2 puffs every 4-6 hours PRN", "1 Inhaler (200 Doses)", "Rinse mouth with water after inhalation. Use spacer device.");

				createRx(prescriptionRepository, patients.get(3).getId(), doctors.get(3).getId(),
						"Ibuprofen", "400 mg - Every 8 hours as needed", "5 Days", "Take with meals or milk. Do not exceed 1200mg in 24 hours.");

				createRx(prescriptionRepository, patients.get(4).getId(), doctors.get(4).getId(),
						"Hydrocortisone 1% Cream", "Apply thin film to affected areas twice daily", "14 Days", "Wash hands before and after applying. For external dermatological use only.");

				createRx(prescriptionRepository, patients.get(5).getId(), doctors.get(5).getId(),
						"Metformin Hydrochloride", "500 mg - Twice daily with meals", "60 Days", "Take immediately after breakfast and dinner to minimize GI upset.");

				createRx(prescriptionRepository, patients.get(6).getId(), doctors.get(6).getId(),
						"Ondansetron", "4 mg - Every 8 hours as needed for nausea", "10 Days", "Dissolve on tongue. Keep well hydrated.");

				createRx(prescriptionRepository, patients.get(7).getId(), doctors.get(7).getId(),
						"Prenatal Multivitamin with Folic Acid", "1 Tablet - Once daily in morning", "90 Days", "Take with breakfast or fruit juice.");

				createRx(prescriptionRepository, patients.get(8).getId(), doctors.get(8).getId(),
						"Carboxymethylcellulose 0.5% Eye Drops", "1-2 drops in each eye 4 times daily", "30 Days", "Discard bottle 30 days after opening. Do not touch dropper tip.");

				createRx(prescriptionRepository, patients.get(9).getId(), doctors.get(9).getId(),
						"Escitalopram Oxalate", "10 mg - Once daily in morning", "30 Days", "Consistent timing daily. Do not discontinue abruptly without physician consultation.");

				createRx(prescriptionRepository, patients.get(10).getId(), doctors.get(0).getId(),
						"Atorvastatin Calcium", "20 mg - Once daily at bedtime", "90 Days", "Take after dinner. Routine lipid profile check scheduled in 3 months.");
			}

			// 7. Ensure ANY currently existing patients (e.g. self-registered users) also have clinical records
			List<User> allPatients = userRepository.findByRole("PATIENT");
			for (User p : allPatients) {
				if (appointmentRepository.findByPatientId(p.getId()).isEmpty()) {
					createAppointment(appointmentRepository, p.getId(), doctors.get(0).getId(), "2026-10-06T10:00", "Comprehensive health checkup & vitals screening", "CONFIRMED");
					createAppointment(appointmentRepository, p.getId(), doctors.get(1).getId(), "2026-10-18T14:30", "Specialist consultation follow-up", "PENDING");
				}
				if (medicalRecordRepository.findByPatientId(p.getId()).isEmpty()) {
					createRecord(medicalRecordRepository, p.getId(), doctors.get(0).getId(),
							"Routine Health Assessment - Optimal",
							"Vitals: BP 120/80 mmHg, Pulse 72 bpm, SpO2 99%. General physical examination within normal limits. Advised continued balanced diet and hydration.");
				}
				if (prescriptionRepository.findByPatientId(p.getId()).isEmpty()) {
					createRx(prescriptionRepository, p.getId(), doctors.get(0).getId(),
							"Multivitamin & Mineral Complex", "1 Capsule daily after breakfast", "30 Days",
							"Take with water. Dietary supplement for general wellness.");
				}
			}

			System.out.println(">>> CareConnect Data Seeder: Initialized database with realistic clinical records and BCrypt password encryption!");
		};
	}

	private User getOrCreateDoctor(UserRepository repo, PasswordEncoder encoder, String name, String email, String password, String spec, String phone, String days, String time, String status) {
		Optional<User> existing = repo.findByEmail(email);
		if (existing.isPresent()) {
			return existing.get();
		}
		User doc = new User(name, email, encoder.encode(password), "DOCTOR");
		doc.setSpecialization(spec);
		doc.setPhone(phone);
		doc.setAvailableDays(days);
		doc.setAvailableTime(time);
		doc.setAvailabilityStatus(status);
		return repo.save(doc);
	}

	private User getOrCreatePatient(UserRepository repo, PasswordEncoder encoder, String name, String email, String password, String phone) {
		Optional<User> existing = repo.findByEmail(email);
		if (existing.isPresent()) {
			return existing.get();
		}
		User patient = new User(name, email, encoder.encode(password), "PATIENT");
		patient.setPhone(phone);
		return repo.save(patient);
	}

	private void createAppointment(AppointmentRepository repo, Long patientId, Long doctorId, String date, String reason, String status) {
		Appointment appt = new Appointment();
		appt.setPatientId(patientId);
		appt.setDoctorId(doctorId);
		appt.setAppointmentDate(date);
		appt.setReason(reason);
		appt.setStatus(status);
		repo.save(appt);
	}

	private void createRecord(MedicalRecordRepository repo, Long patientId, Long doctorId, String diagnosis, String notes) {
		MedicalRecord r = new MedicalRecord();
		r.setPatientId(patientId);
		r.setDoctorId(doctorId);
		r.setDiagnosis(diagnosis);
		r.setClinicalNotes(notes);
		repo.save(r);
	}

	private void createRx(PrescriptionRepository repo, Long patientId, Long doctorId, String medicine, String dosage, String duration, String instructions) {
		Prescription rx = new Prescription();
		rx.setPatientId(patientId);
		rx.setDoctorId(doctorId);
		rx.setMedicineName(medicine);
		rx.setDosage(dosage);
		rx.setDuration(duration);
		rx.setInstructions(instructions);
		repo.save(rx);
	}
}
