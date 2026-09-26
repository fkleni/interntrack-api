package com.interntrack.api.service;

import com.interntrack.api.dto.DashboardStats;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.User;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final GeminiService geminiService;

    public ApplicationService(ApplicationRepository repository,
                              UserRepository userRepository,
                              FileStorageService fileStorageService,
                              GeminiService geminiService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.geminiService = geminiService;
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username));
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Application saveApplication(Application application) {
        application.setOwner(getCurrentUser());
        return repository.save(application);
    }

    public List<Application> getAllApplications() {
        return repository.findByOwner(getCurrentUser());
    }

    public Application getApplicationById(Long id) {
        return repository.findByIdAndOwner(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + id));
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Application updateApplication(Long id, Application updated) {
        Application existing = getApplicationById(id);

        boolean jobDescriptionChanged =
                !java.util.Objects.equals(existing.getJobDescription(), updated.getJobDescription());

        existing.setCompanyName(updated.getCompanyName());
        existing.setPosition(updated.getPosition());
        existing.setStatus(updated.getStatus());
        existing.setAppliedDate(updated.getAppliedDate());
        existing.setNotes(updated.getNotes());
        existing.setJobDescription(updated.getJobDescription());

        if (jobDescriptionChanged) {
            existing.setAiInsight(null);
            existing.setAiInsightGeneratedAt(null);
        }

        return repository.save(existing);
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteApplication(Long id) {
        Application existing = getApplicationById(id);
        if (existing.getCvFilePath() != null) {
            fileStorageService.delete(existing.getCvFilePath());
        }
        repository.delete(existing);
    }

    @Cacheable(value = "dashboardStats", key = "#root.target.getCurrentUser().username")
    public DashboardStats getDashboardStats() {
        List<Application> all = repository.findByOwner(getCurrentUser());

        Map<String, Long> statusCounts = all.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        return new DashboardStats(all.size(), statusCounts);
    }

    public Application uploadCv(Long id, MultipartFile file) {
        Application application = getApplicationById(id);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        String previousPath = application.getCvFilePath();

        String storedPath = fileStorageService.store(file, application.getId());
        application.setCvFilePath(storedPath);
        application.setAiInsight(null);
        application.setAiInsightGeneratedAt(null);

        Application saved = repository.save(application);

        if (previousPath != null) {
            fileStorageService.delete(previousPath);
        }

        return saved;
    }

    public void deleteCv(Long id) {
        Application application = getApplicationById(id);

        if (application.getCvFilePath() == null) {
            throw new ResourceNotFoundException("This application has no CV file to delete");
        }

        fileStorageService.delete(application.getCvFilePath());
        application.setCvFilePath(null);
        application.setAiInsight(null);
        application.setAiInsightGeneratedAt(null);
        repository.save(application);
    }

    public Resource downloadCv(Long id) {
        Application application = getApplicationById(id);

        if (application.getCvFilePath() == null) {
            throw new ResourceNotFoundException("This application has no CV file uploaded");
        }

        return fileStorageService.loadAsResource(application.getCvFilePath());
    }

    public Application analyzeApplication(Long id) {
        Application application = getApplicationById(id);

        if (application.getJobDescription() == null || application.getJobDescription().isBlank()) {
            throw new IllegalArgumentException("This application has no job description to analyze against");
        }
        if (application.getCvFilePath() == null) {
            throw new IllegalArgumentException("This application has no CV uploaded to analyze");
        }

        if (application.getAiInsight() != null) {
            return application;
        }

        byte[] cvBytes = fileStorageService.downloadBytes(application.getCvFilePath());

        String prompt = """
                Compare the technical requirements in the following job description with the attached CV.
                List the 2-3 most important missing skills or gaps, and briefly explain why each matters
                for this specific role. Be concise and specific.

                Job description:
                %s
                """.formatted(application.getJobDescription());

        String insight = geminiService.analyzeJobFit(prompt, cvBytes);

        application.setAiInsight(insight);
        application.setAiInsightGeneratedAt(LocalDateTime.now());

        return repository.save(application);
    }
}