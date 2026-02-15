package com.insurance.products.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentStorageService {

    /**
     * Store a file and return the storage path
     */
    String storeFile(MultipartFile file, String directory) throws IOException;

    /**
     * Delete a file by its storage path
     */
    void deleteFile(String storagePath) throws IOException;

    /**
     * Get file content as InputStream
     */
    InputStream getFile(String storagePath) throws IOException;

    /**
     * Check if a file exists
     */
    boolean fileExists(String storagePath);

    /**
     * Get the public URL for a file (if applicable)
     */
    String getFileUrl(String storagePath);
}
