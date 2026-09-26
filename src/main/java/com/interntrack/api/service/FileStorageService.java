package com.interntrack.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Cloudinary cloudinary;

    public FileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String store(MultipartFile file, Long applicationId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        String publicId = "interntrack/app-" + applicationId + "-" + UUID.randomUUID();

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "resource_type", "raw"
                    )
            );
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    public Resource loadAsResource(String fileUrl) {
        try {
            Resource resource = new UrlResource(URI.create(fileUrl));
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + fileUrl);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file URL: " + fileUrl, e);
        }
    }

    public byte[] downloadBytes(String fileUrl) {
        try {
            return new URL(fileUrl).openStream().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file from storage: " + fileUrl, e);
        }
    }

    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }
        try {
            String publicId = extractPublicId(fileUrl);
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", fileUrl, e);
        }
    }

    private String extractPublicId(String secureUrl) {
        int uploadIndex = secureUrl.indexOf("/upload/");
        String afterUpload = secureUrl.substring(uploadIndex + "/upload/".length());
        return afterUpload.replaceFirst("^v\\d+/", "");
    }
}