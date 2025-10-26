package org.example.dinedelightsystems.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendLoginSuccessEmail(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom("venurajayasingha1@gmail.com");
        message.setSubject("Login Successful - Dine & Delight");
        message.setText("You have successfully logged in to Dine & Delight.");
        mailSender.send(message);
    }
}



