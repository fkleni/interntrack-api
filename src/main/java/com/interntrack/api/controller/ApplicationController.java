package com.interntrack.api.controller;

import com.interntrack.api.entity.Application;
import com.interntrack.api.service.ApplicationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }
    @PostMapping
    public Application createApplication(@RequestBody Application application) {
        return service.saveApplication(application);

    }
}
