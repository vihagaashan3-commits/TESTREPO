package com.roadrescue.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir = Paths.get("uploads/vehicles");

    public FileStorageService() {
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    /**
     * Stores the uploaded file to disk and returns a web-accessible path.
     * Returns null if no file was provided (so callers can skip overwriting).
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/vehicles/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }
}