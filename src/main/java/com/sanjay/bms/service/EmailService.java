package com.sanjay.bms.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;


import sendinblue.ApiClient;
import sendinblue.Configuration;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@AllArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private static final String FROM_EMAIL = "sanjaygoud902@gmail.com";

    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        // Try Brevo API first
        try {
            sendViaBrevoApi(to, subject, htmlContent);
            return; // Success, exit early
        } catch (Exception e) {
            log.warn("⚠️ Brevo API failed, trying SMTP fallback: {}", e.getMessage());
        }

        // Fallback to SMTP if API fails
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ HTML email sent via SMTP to: {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send HTML email via SMTP to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email sending completely failed", e);
        }
    }

    private void sendViaBrevoApi(String to, String subject, String htmlContent) {
        try {
            // Get API key from environment variable
            String apiKey = System.getenv("BREVO_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("BREVO_API_KEY not configured");
            }

            // Configure API client
            ApiClient defaultClient = Configuration.getDefaultApiClient();
            ApiKeyAuth auth = (ApiKeyAuth) defaultClient.getAuthentication("api-key");
            auth.setApiKey(apiKey);

            // Create email API instance
            TransactionalEmailsApi api = new TransactionalEmailsApi();

            // Set sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail(FROM_EMAIL);
            sender.setName("Banking App");

            // Set recipient
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail(to);

            // Build email
            SendSmtpEmail email = new SendSmtpEmail();
            email.setSender(sender);
            email.setTo(Collections.singletonList(recipient));
            email.setSubject(subject);
            email.setHtmlContent(htmlContent);

            // Send email
            CreateSmtpEmail response = api.sendTransacEmail(email);
            log.info("✅ Email sent via Brevo API to: {} (Message ID: {})", to, response.getMessageId());

        } catch (Exception e) {
            log.error("❌ Failed to send email via Brevo API: {}", e.getMessage(), e);
            throw new RuntimeException("Brevo API email failed", e);
        }
    }


    @Async
    public void sendWelcomeEmail(String to, String fullName, String username) {
        String subject = "Welcome to Banking App!";
        String htmlContent = buildWelcomeEmailTemplate(fullName, username);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendTransactionAlert(String to, String accountNumber, String transactionType,
                                     BigDecimal amount, BigDecimal balance, String referenceNumber) {
        String subject = "Transaction Alert - " + transactionType;
        String htmlContent = buildTransactionAlertTemplate(accountNumber, transactionType,
                amount, balance, referenceNumber);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendOtpEmail(String to, String otpCode, String purpose, int expiryMinutes) {
        String subject = "Your OTP Code - Banking App";
        String htmlContent = buildOtpEmailTemplate(otpCode, purpose, expiryMinutes);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendAccountStatusEmail(String to, String accountNumber, String status, String reason) {
        String subject = "Account Status Update - Banking App";
        String htmlContent = buildAccountStatusTemplate(accountNumber, status, reason);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Async
    public void sendPasswordChangeConfirmation(String to, String username) {
        String subject = "Password Changed Successfully";
        String htmlContent = buildPasswordChangeTemplate(username);
        sendHtmlEmail(to, subject, htmlContent);
    }

    // Email Templates
    private String buildWelcomeEmailTemplate(String fullName, String username) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to Banking App!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s,</h2>
                        <p>Thank you for registering with Banking App. Your account has been created successfully.</p>
                        <p><strong>Username:</strong> %s</p>
                        <p>You can now log in and start managing your accounts.</p>
                        <p>If you have any questions, please don't hesitate to contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Banking App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, fullName, username);
    }

    private String buildTransactionAlertTemplate(String accountNumber, String transactionType,
                                                 BigDecimal amount, BigDecimal balance, String referenceNumber) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedDate = LocalDateTime.now().format(formatter);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .transaction-details { background-color: white; padding: 15px; border-left: 4px solid #2196F3; }
                    .amount { font-size: 24px; font-weight: bold; color: #2196F3; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Transaction Alert</h1>
                    </div>
                    <div class="content">
                        <h2>Transaction Successful</h2>
                        <div class="transaction-details">
                            <p><strong>Account:</strong> %s</p>
                            <p><strong>Transaction Type:</strong> %s</p>
                            <p><strong>Amount:</strong> <span class="amount">₹%s</span></p>
                            <p><strong>Current Balance:</strong> ₹%s</p>
                            <p><strong>Reference Number:</strong> %s</p>
                            <p><strong>Date & Time:</strong> %s</p>
                        </div>
                        <p style="margin-top: 20px; color: #666;">If you did not authorize this transaction, please contact us immediately.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Banking App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, maskAccountNumber(accountNumber), transactionType, amount, balance, referenceNumber, formattedDate);
    }

    private String buildOtpEmailTemplate(String otpCode, String purpose, int expiryMinutes) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #FF9800; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .otp-box { background-color: white; padding: 30px; text-align: center; border: 2px dashed #FF9800; margin: 20px 0; }
                    .otp-code { font-size: 36px; font-weight: bold; color: #FF9800; letter-spacing: 5px; }
                    .warning { background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>OTP Verification</h1>
                    </div>
                    <div class="content">
                        <h2>Your One-Time Password (OTP)</h2>
                        <p>Your OTP for <strong>%s</strong> is:</p>
                        <div class="otp-box">
                            <div class="otp-code">%s</div>
                        </div>
                        <p>This OTP will expire in <strong>%d minutes</strong>.</p>
                        <div class="warning">
                            <strong>Security Warning:</strong>
                            <ul>
                                <li>Never share your OTP with anyone</li>
                                <li>Banking App will never ask for your OTP via phone or email</li>
                                <li>If you did not request this OTP, please contact support immediately</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Banking App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, purpose, otpCode, expiryMinutes);
    }

    private String buildAccountStatusTemplate(String accountNumber, String status, String reason) {
        String color = status.equals("Frozen") ? "#f44336" : "#4CAF50";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: %s; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .status-box { background-color: white; padding: 20px; border-left: 4px solid %s; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Account Status Update</h1>
                    </div>
                    <div class="content">
                        <h2>Important Notice</h2>
                        <div class="status-box">
                            <p><strong>Account Number:</strong> %s</p>
                            <p><strong>Status:</strong> %s</p>
                            <p><strong>Reason:</strong> %s</p>
                        </div>
                        <p style="margin-top: 20px;">If you have any questions or concerns, please contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Banking App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, color, color, maskAccountNumber(accountNumber), status, reason);
    }

    private String buildPasswordChangeTemplate(String username) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedDate = LocalDateTime.now().format(formatter);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .warning { background-color: #ffebee; border-left: 4px solid #f44336; padding: 15px; margin: 20px 0; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Changed</h1>
                    </div>
                    <div class="content">
                        <h2>Password Successfully Updated</h2>
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Your password has been changed successfully on <strong>%s</strong>.</p>
                        <div class="warning">
                            <strong>Security Alert:</strong>
                            <p>If you did not make this change, please contact our support team immediately and secure your account.</p>
                        </div>
                        <p>For your security, we recommend:</p>
                        <ul>
                            <li>Use a strong, unique password</li>
                            <li>Enable two-factor authentication</li>
                            <li>Never share your password with anyone</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>&copy; 2025 Banking App. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, username, formattedDate);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }
}