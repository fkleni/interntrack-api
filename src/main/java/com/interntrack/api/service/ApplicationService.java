package com.interntrack.api.service;

import com.interntrack.api.entity.Application;
import com.interntrack.api.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }
    public Application saveApplication(Application application) {
        return repository.save(application);
    }
}
