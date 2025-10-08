package com.sanjay.bms.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    public ValidationResult validate(String password) {
        ValidationResult result = new ValidationResult();

        if (password == null || password.isEmpty()) {
            result.setValid(false);
            result.addError("Password cannot be empty");
            return result;
        }

        // Check length
        if (password.length() < MIN_LENGTH) {
            result.setValid(false);
            result.addError("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password.length() > MAX_LENGTH) {
            result.setValid(false);
            result.addError("Password must not exceed " + MAX_LENGTH + " characters");
        }

        // Check for uppercase
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one uppercase letter");
        }

        // Check for lowercase
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one lowercase letter");
        }

        // Check for digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one digit");
        }

        // Check for special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            result.setValid(false);
            result.addError("Password must contain at least one special character");
        }

        // Check for common passwords
        if (isCommonPassword(password)) {
            result.setValid(false);
            result.addError("Password is too common. Please choose a stronger password");
        }

        return result;
    }

    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
                "password", "12345678", "qwerty", "abc123", "password123",
                "admin123", "letmein", "welcome", "monkey", "dragon"
        };

        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                return true;
            }
        }
        return false;
    }

    public static class ValidationResult {
        private boolean valid = true;
        private java.util.List<String> errors = new java.util.ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}