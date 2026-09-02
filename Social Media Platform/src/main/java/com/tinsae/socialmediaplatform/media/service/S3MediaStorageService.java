package com.tinsae.socialmediaplatform.media.service;

import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.media.config.S3StorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class S3MediaStorageService {

    private static final Path LOCAL_UPLOAD_DIRECTORY = Path.of("uploads");
    private static final String LOCAL_PUBLIC_BASE_URL = "http://localhost:8080/uploads";

    private final S3Client s3Client;
    private final S3StorageProperties properties;

    public S3MediaStorageService(S3Client s3Client, S3StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public StoredMedia upload(MultipartFile file) {
        if (isBlank(properties.getBucket())) {
            return uploadLocally(file);
        }

        validateS3Configuration();

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String key = buildObjectKey(file.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new StoredMedia(key, publicUrl(key));
        } catch (IOException exception) {
            throw new BusinessRuleException("Unable to read uploaded media file.");
        } catch (S3Exception exception) {
            throw new BusinessRuleException("Unable to upload media file to S3.");
        }
    }

    private StoredMedia uploadLocally(MultipartFile file) {
        String key = buildLocalObjectKey(file.getOriginalFilename());
        Path uploadDirectory = LOCAL_UPLOAD_DIRECTORY.toAbsolutePath().normalize();
        Path destination = uploadDirectory.resolve(key).normalize();

        try {
            Files.createDirectories(uploadDirectory);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredMedia(key, LOCAL_PUBLIC_BASE_URL + "/" + key);
        } catch (IOException exception) {
            throw new BusinessRuleException("Unable to store uploaded media file.");
        }
    }

    private void validateS3Configuration() {
        if (isBlank(properties.getRegion())) {
            throw new BusinessRuleException("AWS S3 region is not configured.");
        }
    }

    private String buildObjectKey(String originalFilename) {
        String prefix = normalizePrefix(properties.getKeyPrefix());
        String filename = cleanFilename(originalFilename);
        return prefix + UUID.randomUUID() + "-" + filename;
    }

    private String buildLocalObjectKey(String originalFilename) {
        return UUID.randomUUID() + "-" + cleanFilename(originalFilename);
    }

    private String publicUrl(String key) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return trimTrailingSlash(properties.getPublicBaseUrl()) + "/" + key;
        }

        return "https://" + properties.getBucket() + ".s3." + properties.getRegion() + ".amazonaws.com/" + key;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }

        String normalized = prefix.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        return normalized;
    }

    private String cleanFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record StoredMedia(String key, String url) {
    }
}
