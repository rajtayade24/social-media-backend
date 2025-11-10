package com.projects.instagram.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    // base directory for storing uploaded files, configure in application.properties
    private final Path storageDir;

    public FileStorageService(@Value("${app.storage.dir:uploads}") String storageDirPath) {
        this.storageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    /**
     * Saves the file to disk and returns the saved relative path (or URL).
     * - Generates a UUID filename to avoid clashes.
     * - Preserves original extension when possible.
     */
    public String storeFile(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) ext = original.substring(idx);

        String filename = UUID.randomUUID().toString() + ext;

        Path target = this.storageDir.resolve(filename);
        try {
            // Save the file
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Return a relative path (you can make it a full URL if you serve files with mapping)
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file " + filename, e);
        }
    }

    /**
     * Build a file system path from stored filename
     */
    public Path loadPath(String filename) {
        return this.storageDir.resolve(filename).normalize();
    }
}
