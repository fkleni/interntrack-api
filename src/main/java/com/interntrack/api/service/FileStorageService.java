package com.interntrack.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path storageRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + storageRoot, e);
        }
    }

    public String store(MultipartFile file, Long applicationId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid file name: " + originalFilename);
        }

        String storedFilename = "app-" + applicationId + "-" + UUID.randomUUID() + ".pdf";

        try {
            Path targetPath = storageRoot.resolve(storedFilename).normalize();

            if (!targetPath.getParent().equals(storageRoot)) {
                throw new IllegalArgumentException("Cannot store file outside the designated directory");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    public Resource loadAsResource(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize();
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path: " + filePath, e);
        }
    }

    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            Path path = Paths.get(filePath).normalize();
            boolean deleted = Files.deleteIfExists(path);
            if (!deleted) {
                log.warn("Tried to delete file but it did not exist: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }
}