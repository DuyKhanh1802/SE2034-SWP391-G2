package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendResetPasswordOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel - Password Reset OTP");
            message.setText(
                    "Hello,\n\n" +
                            "We received a request to reset your ViHotel account password.\n\n" +
                            "Your OTP code is: " + otp + "\n\n" +
                            "This OTP will expire in 5 minutes.\n\n" +
                            "If you did not request this action, please ignore this email.\n\n" +
                            "ViHotel Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send reset password OTP email: " + e.getMessage(), e);
        }
    }

    public void sendRoomCodeEmail(String toEmail,
                                  String guestName,
                                  String bookingReference,
                                  String roomNumber,
                                  String roomCode,
                                  String checkInDate,
                                  String checkOutDate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel Hanoi - Your Room Code");

            message.setText(
                    "Hello " + guestName + ",\n\n" +
                            "Welcome to ViHotel Hanoi.\n\n" +
                            "Your booking reference is: " + bookingReference + "\n" +
                            "Room Number: " + roomNumber + "\n" +
                            "Your Room Code: " + roomCode + "\n\n" +
                            "Check-in Date: " + checkInDate + "\n" +
                            "Check-out Date: " + checkOutDate + "\n\n" +
                            "Please keep this code private. You can use this room code to access the guest portal during your stay.\n\n" +
                            "ViHotel Hanoi Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send room code email: " + e.getMessage(), e);
        }
    }

    public void sendRoomCodesEmail(String toEmail,
                                   String guestName,
                                   String bookingReference,
                                   String emailContent) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("ViHotel Hanoi - Your Room Access Code");
            message.setText(
                    "Hello " + guestName + ",\n\n" +
                            "Welcome to ViHotel Hanoi.\n\n" +
                            "Your booking reference is: " + bookingReference + "\n\n" +
                            emailContent + "\n\n" +
                            "Please keep your room code private. You can use it to access the guest portal during your stay.\n\n" +
                            "ViHotel Hanoi Team"
            );

            mailSender.send(message);

        } catch (MailException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send room code email: " + e.getMessage(), e);
        }
    }

    
}