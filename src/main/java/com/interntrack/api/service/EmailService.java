package com.interntrack.api.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminderEmail(String to, String companyName, LocalDate interviewDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Interview Reminder: " + companyName);
        message.setText(
                "Hi,\n\nThis is a reminder that you have an interview with "
                        + companyName + " on " + interviewDate + ".\n\nGood luck!\n\nInternTrack API :)"
        );

        mailSender.send(message);
    }
}
