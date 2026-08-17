package com.interntrack.api.scheduler;

import com.interntrack.api.entity.Application;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class InterviewReminderScheduler {

    private final ApplicationRepository repository;
    private final EmailService emailService;

    public InterviewReminderScheduler(ApplicationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000)
    public void sendInterviewReminders() {
        System.out.println("Scheduler triggered at: " + java.time.LocalDateTime.now());

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Application> upcoming = repository.findAll().stream()
                .filter(app -> app.getInterviewDate() != null)
                .filter(app -> app.getInterviewDate().equals(today) || app.getInterviewDate().equals(tomorrow))
                .filter(app -> app.getLastReminderSentDate() == null || !app.getLastReminderSentDate().equals(today))
                .toList();

        System.out.println("Found " + upcoming.size() + " applications to remind.");

        for (Application app : upcoming) {
            try {
                emailService.sendReminderEmail(
                        "projefadime@gmail.com",
                        app.getCompanyName(),
                        app.getInterviewDate()
                );
                System.out.println("Email sent successfully for application id: " + app.getId());
                app.setLastReminderSentDate(today);
                repository.save(app);
                Thread.sleep(5000);
            } catch (Exception e) {
                System.out.println("Failed to send email for application id: " + app.getId() + " - Error: " + e.getMessage());
            }
        }
    }
}