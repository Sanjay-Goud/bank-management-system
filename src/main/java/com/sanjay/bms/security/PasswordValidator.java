// PasswordValidator.java
package com.sanjay.bms.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    // Regex patterns
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

    public ValidationResult validate(String password) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "Password cannot be empty");
        }

        if (password.length() < MIN_LENGTH) {
            return new ValidationResult(false,
                    String.format("Password must be at least %d characters long", MIN_LENGTH));
        }

        if (password.length() > MAX_LENGTH) {
            return new ValidationResult(false,
                    String.format("Password must not exceed %d characters", MAX_LENGTH));
        }

        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return new ValidationResult(false,
                    "Password must contain at least one uppercase letter");
        }

        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return new ValidationResult(false,
                    "Password must contain at least one lowercase letter");
        }

        if (!DIGIT_PATTERN.matcher(password).find()) {
            return new ValidationResult(false,
                    "Password must contain at least one digit");
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return new ValidationResult(false,
                    "Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)");
        }

        // Check for common weak passwords
        if (isCommonPassword(password)) {
            return new ValidationResult(false,
                    "Password is too common. Please choose a stronger password");
        }

        return new ValidationResult(true, "Password is valid");
    }

    private boolean isCommonPassword(String password) {
        String lowerPassword = password.toLowerCase();
        String[] commonPasswords = {
                "password", "12345678", "qwerty", "abc123", "password123",
                "admin123", "letmein", "welcome", "monkey", "dragon"
        };

        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                return true;
            }
        }

        return false;
    }

    public String getPasswordRequirements() {
        return String.format(
                "Password must:\n" +
                        "- Be at least %d characters long\n" +
                        "- Contain at least one uppercase letter\n" +
                        "- Contain at least one lowercase letter\n" +
                        "- Contain at least one digit\n" +
                        "- Contain at least one special character\n" +
                        "- Not be a common password",
                MIN_LENGTH
        );
    }

    @Getter
    @AllArgsConstructor
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
    }
}