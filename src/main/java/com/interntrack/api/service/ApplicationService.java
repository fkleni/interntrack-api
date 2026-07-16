package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }
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

    public Application updateApplication(Long id, Application updated) {
        Application existing = getApplicationById(id);
        existing.setCompanyName(updated.getCompanyName());
        existing.setPosition(updated.getPosition());
        existing.setStatus(updated.getStatus());
        existing.setAppliedDate(updated.getAppliedDate());
        existing.setNotes(updated.getNotes());

        return repository.save(existing);
    }

    public void deleteApplication(Long id) {
        Application existing = getApplicationById(id);
        repository.delete(existing);
    }
}
