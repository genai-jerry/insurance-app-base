package com.insurance.products.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements DocumentStorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-path}") String storagePath) {
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
            log.info("Storage location initialized at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + this.rootLocation, e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String directory) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        if (originalFilename.contains("..")) {
            throw new IOException("Invalid file path: " + originalFilename);
        }

        // Generate unique filename
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Create directory if it doesn't exist
        Path directoryPath = this.rootLocation.resolve(directory);
        Files.createDirectories(directoryPath);

        // Store file
        Path targetLocation = directoryPath.resolve(uniqueFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        // Return relative path from root location
        String storagePath = directory + "/" + uniqueFilename;
        log.info("File stored successfully: {}", storagePath);
        return storagePath;
    }

    @Override
    public void deleteFile(String storagePath) throws IOException {
        Path filePath = this.rootLocation.resolve(storagePath).normalize();

        if (!filePath.startsWith(this.rootLocation)) {
            throw new IOException("Invalid file path: " + storagePath);
        }

        Files.deleteIfExists(filePath);
        log.info("File deleted: {}", storagePath);
    }

    @Override
    public InputStream getFile(String storagePath) throws IOException {
        Path filePath = this.rootLocation.resolve(storagePath).normalize();

        if (!filePath.startsWith(this.rootLocation)) {
            throw new IOException("Invalid file path: " + storagePath);
        }

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + storagePath);
        }

        return Files.newInputStream(filePath);
    }

    @Override
    public boolean fileExists(String storagePath) {
        try {
            Path filePath = this.rootLocation.resolve(storagePath).normalize();
            return filePath.startsWith(this.rootLocation) && Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getFileUrl(String storagePath) {
        // For local storage, we'll use the download endpoint
        return "/api/products/documents/download/" + storagePath;
    }
}
