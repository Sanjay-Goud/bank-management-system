package com.sanjay.bms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class TwoFactorAuthService {

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String generateQRCodeUrl(String username, String secret) {
        String issuer = "BankingApp";
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, username,
                new GoogleAuthenticatorKey.Builder(secret).build());
    }

    public byte[] generateQRCodeImage(String qrCodeUrl, int width, int height)
            throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    public String generateQRCodeBase64(String qrCodeUrl) {
        try {
            byte[] qrCodeImage = generateQRCodeImage(qrCodeUrl, 200, 200);
            return Base64.getEncoder().encodeToString(qrCodeImage);
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    public boolean verifyCode(String secret, String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            return verifyCode(secret, code);
        } catch (NumberFormatException e) {
            log.error("Invalid 2FA code format: {}", codeStr);
            return false;
        }
    }

    public String[] generateBackupCodes(int count) {
        String[] backupCodes = new String[count];
        for (int i = 0; i < count; i++) {
            backupCodes[i] = generateBackupCode();
        }
        return backupCodes;
    }

    private String generateBackupCode() {
        // Generate 8-character backup code
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing characters
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
            if (i == 3) code.append("-"); // Format: XXXX-XXXX
        }
        return code.toString();
    }
}