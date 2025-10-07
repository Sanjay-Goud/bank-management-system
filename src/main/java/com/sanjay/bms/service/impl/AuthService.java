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
        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
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

        // Generate token
        String token = jwtUtil.generateToken(savedUser.getUsername());

        // Convert to DTO
        UserDto userDto = mapToUserDto(savedUser);

        return new AuthResponse(token, userDto);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                });

        log.info("User found: {}", user.getUsername());
        log.info("User enabled: {}", user.getEnabled());

        // Check if user is enabled
        if (!user.getEnabled()) {
            log.error("Account disabled for user: {}", user.getUsername());
            throw new IllegalArgumentException("Account is disabled");
        }

        // Verify password
        log.info("Verifying password...");
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        log.info("Password matches: {}", passwordMatches);

        if (!passwordMatches) {
            log.error("Invalid password for user: {}", user.getUsername());
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("Login successful for user: {}", user.getUsername());

        // Generate token
        String token = jwtUtil.generateToken(user.getUsername());

        // Convert to DTO
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