package com.interntrack.api.controller;

import com.interntrack.api.dto.DashboardStats;
import com.interntrack.api.entity.Application;
import com.interntrack.api.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationService service;

    public ApplicationController(ApplicationService service) {
        this.service = service;
    }
    @PostMapping
    public ResponseEntity<Application> createApplication(@Valid @RequestBody Application application) {
        Application saved = service.saveApplication(application);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<Application>> getAllApplications() {
        return ResponseEntity.ok(service.getAllApplications());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStats> getDashboard() {
        return ResponseEntity.ok(service.getDashboardStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getApplicationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> update(@PathVariable Long id,@Valid @RequestBody Application application) {
        return ResponseEntity.ok(service.updateApplication(id, application));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/upload-cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Application> uploadCv(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Application updated = service.uploadCv(id, file);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/delete-cv")
    public ResponseEntity<Void> deleteCv(@PathVariable Long id) {
        service.deleteCv(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download-cv")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        Resource resource = service.downloadCv(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cv-" + id + ".pdf\"")
                .body(resource);
    }
}