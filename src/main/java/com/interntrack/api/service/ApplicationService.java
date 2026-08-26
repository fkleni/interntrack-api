package com.interntrack.api.service;

import com.interntrack.api.dto.DashboardStats;
import com.interntrack.api.entity.Application;
import com.interntrack.api.entity.User;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import com.interntrack.api.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;
    private final UserRepository userRepository;

    public ApplicationService(ApplicationRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
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
        existing.setCompanyName(updated.getCompanyName());
        existing.setPosition(updated.getPosition());
        existing.setStatus(updated.getStatus());
        existing.setAppliedDate(updated.getAppliedDate());
        existing.setNotes(updated.getNotes());

        return repository.save(existing);
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public void deleteApplication(Long id) {
        Application existing = getApplicationById(id);
        repository.delete(existing);
    }

    @Cacheable(value = "dashboardStats", key = "#root.target.getCurrentUser().username")
    public DashboardStats getDashboardStats() {
        List<Application> all = repository.findByOwner(getCurrentUser());

        Map<String, Long> statusCounts = all.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        return new DashboardStats(all.size(), statusCounts);
    }
}