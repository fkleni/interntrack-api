package com.interntrack.api.scheduler;

import com.interntrack.api.entity.Application;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class InterviewReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(InterviewReminderScheduler.class);

    private final ApplicationRepository repository;
    private final EmailService emailService;

    public InterviewReminderScheduler(ApplicationRepository repository, EmailService emailService) {
        this.repository = repository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void sendInterviewReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<Application> upcoming = repository.findAll().stream()
                .filter(app -> app.getInterviewDate() != null)
                .filter(app -> app.getInterviewDate().equals(today) || app.getInterviewDate().equals(tomorrow))
                .filter(app -> app.getOwner() != null && app.getOwner().getEmail() != null)
                .filter(app -> !today.equals(app.getLastReminderSentDate()))
                .toList();

        for (Application app : upcoming) {
            try {
                emailService.sendReminderEmail(
                        app.getOwner().getEmail(),
                        app.getCompanyName(),
                        app.getInterviewDate()
                );
                app.setLastReminderSentDate(today);
                repository.save(app);
            } catch (Exception e) {
                log.error("Failed to send interview reminder for application id={}: {}",
                        app.getId(), e.getMessage());
            }
        }
    }
}