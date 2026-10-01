package com.careconnect.careconnect.service;

import com.careconnect.careconnect.model.User;
import com.careconnect.careconnect.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByEmail(email.trim());
    }

    public User registerUser(User user) {
        if (user.getPassword() != null && !isBcryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User loginUser(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user != null) {
            String storedPassword = user.getPassword();

            // 1. Check if stored password is BCrypt hashed
            if (isBcryptHash(storedPassword)) {
                if (passwordEncoder.matches(password, storedPassword)) {
                    return user;
                }
            } else {
                // 2. Backward compatibility for legacy plaintext passwords:
                // If it matches, upgrade the password to BCrypt hash immediately
                if (storedPassword != null && storedPassword.equals(password)) {
                    user.setPassword(passwordEncoder.encode(password));
                    userRepository.save(user);
                    return user;
                }
            }
        }

        return null;
    }

    public PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    private boolean isBcryptHash(String str) {
        if (str == null) return false;
        return str.startsWith("$2a$") || str.startsWith("$2b$") || str.startsWith("$2y$");
    }
}