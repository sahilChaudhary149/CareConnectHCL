package com.careconnect.careconnect;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CareconnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareconnectApplication.class, args);
	}

	@Bean
	public CommandLineRunner initDemoUsers(UserRepository userRepository) {
		return args -> {
			if (userRepository.findByEmail("admin@careconnect.com").isEmpty()) {
				User admin = new User("System Admin", "admin@careconnect.com", "admin123", "ADMIN");
				userRepository.save(admin);
			}

			if (userRepository.findByEmail("doctor@demo.com").isEmpty()) {
				User doctor = new User("Dr. Sarah Smith", "doctor@demo.com", "doctor123", "DOCTOR");
				doctor.setSpecialization("Cardiology");
				doctor.setPhone("+1-555-0199");
				doctor.setAvailableDays("Mon - Fri");
				doctor.setAvailableTime("09:00 AM - 05:00 PM");
				doctor.setAvailabilityStatus("AVAILABLE");
				userRepository.save(doctor);
			}

			if (userRepository.findByEmail("patient@demo.com").isEmpty()) {
				User patient = new User("John Doe", "patient@demo.com", "patient123", "PATIENT");
				patient.setPhone("+1-555-0144");
				userRepository.save(patient);
			}
		};
	}
}
