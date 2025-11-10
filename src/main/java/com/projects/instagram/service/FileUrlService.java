// -----------------------------------------------------------------------------

package com.projects.instagram.service;


import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class FileUrlService {


    /**
     * Convert stored file-reference to absolute URL if needed.
     * Ensures no double-slash issues and uses pathSegment to be safe.
     */
    public String buildUrlIfNeeded(String rawFileUrl) {
        if (rawFileUrl == null || rawFileUrl.isBlank()) return null;
        String lower = rawFileUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return rawFileUrl;
        }
// normalize leading slashes
        String normalized = rawFileUrl.startsWith("/") ? rawFileUrl.substring(1) : rawFileUrl;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(normalized)
                .toUriString();
    }
}