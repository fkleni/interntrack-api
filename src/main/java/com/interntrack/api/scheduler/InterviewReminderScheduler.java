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

    @Scheduled(cron = "0 0 8 * * *")
    public void sendInterviewReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Application> upcoming = repository.findAll().stream()
                .filter(app -> app.getInterviewDate() != null)
                .filter(app -> app.getInterviewDate().equals(today) || app.getInterviewDate().equals(tomorrow))
                .toList();

        for (Application app : upcoming) {
            emailService.sendReminderEmail(
                    "projefadime@gmail.com",
                    app.getCompanyName(),
                    app.getInterviewDate()
            );
        }
    }
}
