package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.repository.UserRepository;
import com.sanjay.bms.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());

        String token = jwtUtil.generateToken(savedUser.getUsername());
        UserDto userDto = mapToUserDto(savedUser);

        return new AuthResponse(token, userDto);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                });

        // Debug logging
        log.debug("User found: {}, Role: {}, Enabled: {}, Locked: {}",
                user.getUsername(), user.getRole(), user.getEnabled(), user.getAccountLocked());

        if (!user.getEnabled()) {
            log.error("Account disabled for user: {}", user.getUsername());
            throw new IllegalArgumentException("Account is disabled");
        }

        if (user.getAccountLocked()) {
            log.error("Account locked for user: {}", user.getUsername());
            throw new IllegalArgumentException("Account is locked");
        }

        // Debug password verification
        log.debug("Verifying password for user: {}", user.getUsername());
        log.debug("Stored password hash length: {}", user.getPassword().length());
        log.debug("Stored password starts with: {}", user.getPassword().substring(0, Math.min(10, user.getPassword().length())));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.debug("Password matches: {}", passwordMatches);

        if (!passwordMatches) {
            log.warn("Failed login attempt for user: {} - Invalid password", user.getUsername());

            // Increment failed login attempts
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // Lock account after 5 failed attempts
            if (user.getFailedLoginAttempts() >= 5) {
                user.setAccountLocked(true);
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(30));
                log.warn("Account locked due to multiple failed login attempts: {}", user.getUsername());
            }

            userRepository.save(user);
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Reset failed login attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful for user: {} with role: {}", user.getUsername(), user.getRole());

        String token = jwtUtil.generateToken(user.getUsername());
        UserDto userDto = mapToUserDto(user);

        return new AuthResponse(token, userDto);
    }

    public UserDto getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToUserDto(user);
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }
}