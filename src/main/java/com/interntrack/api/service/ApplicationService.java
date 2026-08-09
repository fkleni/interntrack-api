package com.interntrack.api.service;

import com.interntrack.api.dto.DashboardStats;
import com.interntrack.api.entity.Application;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    @CacheEvict(value = "dashboardStats", allEntries = true)
    public Application saveApplication(Application application) {
        return repository.save(application);
    }

    public List<Application> getAllApplications() {
        return repository.findAll();
    }

    public Application getApplicationById(Long id) {
        return repository.findById(id)
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

    @Cacheable("dashboardStats")
    public DashboardStats getDashboardStats() {
        List<Application> all = repository.findAll();

        Map<String, Long> statusCounts = all.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));

        return new DashboardStats(all.size(), statusCounts);
    }
}
