package com.sanjay.bms.service;

import com.sanjay.bms.entity.Notification;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender; // Configure in application.properties

    @Transactional
    public void createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyDeposit(User user, String accountNumber, BigDecimal amount) {
        String title = "Deposit Successful";
        String message = String.format("₹%s has been deposited to your account %s",
                amount, maskAccountNumber(accountNumber));
        createNotification(user, title, message, "TRANSACTION");
        sendEmail(user.getEmail(), title, message);
    }

    @Transactional
    public void notifyWithdrawal(User user, String accountNumber, BigDecimal amount) {
        String title = "Withdrawal Successful";
        String message = String.format("₹%s has been withdrawn from your account %s",
                amount, maskAccountNumber(accountNumber));
        createNotification(user, title, message, "TRANSACTION");
        sendEmail(user.getEmail(), title, message);
    }

    @Transactional
    public void notifyTransfer(User fromUser, User toUser, String fromAccount,
                               String toAccount, BigDecimal amount, String referenceNumber) {
        // Notify sender
        String senderTitle = "Transfer Successful";
        String senderMessage = String.format("₹%s transferred to account %s. Ref: %s",
                amount, maskAccountNumber(toAccount), referenceNumber);
        createNotification(fromUser, senderTitle, senderMessage, "TRANSACTION");
        sendEmail(fromUser.getEmail(), senderTitle, senderMessage);

        // Notify receiver
        String receiverTitle = "Money Received";
        String receiverMessage = String.format("₹%s received from account %s. Ref: %s",
                amount, maskAccountNumber(fromAccount), referenceNumber);
        createNotification(toUser, receiverTitle, receiverMessage, "TRANSACTION");
        sendEmail(toUser.getEmail(), receiverTitle, receiverMessage);
    }

    @Transactional
    public void notifyAccountFrozen(User user, String accountNumber, String reason) {
        String title = "Account Frozen";
        String message = String.format("Your account %s has been frozen. Reason: %s. Contact support.",
                maskAccountNumber(accountNumber), reason);
        createNotification(user, title, message, "ACCOUNT");
        sendEmail(user.getEmail(), title, message);
    }

    @Transactional
    public void notifyLoginFromNewDevice(User user, String ipAddress) {
        String title = "New Login Detected";
        String message = String.format("New login to your account from IP: %s at %s",
                ipAddress, LocalDateTime.now());
        createNotification(user, title, message, "SECURITY");
        sendEmail(user.getEmail(), title, message);
    }

    @Transactional
    public void notifyHighValueTransaction(User user, BigDecimal amount, String type) {
        String title = "High Value Transaction Alert";
        String message = String.format("A %s of ₹%s was performed on your account", type, amount);
        createNotification(user, title, message, "SECURITY");
        sendEmail(user.getEmail(), title, message);
    }

    public List<Notification> getUnreadNotifications(User user) {
        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    public List<Notification> getAllNotifications(User user) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> notifications = notificationRepository.findByUserAndIsReadFalse(user);
        notifications.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        });
        notificationRepository.saveAll(notifications);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@bankingapp.com");
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
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