package com.tinsae.socialmediaplatform.media.service;

import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.media.config.S3StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3MediaStorageServiceTest {

    private S3Client s3Client;
    private S3StorageProperties properties;
    private S3MediaStorageService storageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        properties = configuredProperties();
        storageService = new S3MediaStorageService(s3Client, properties);
    }

    @Test
    void uploadStoresObjectWithConfiguredPublicBaseUrlAndNormalizedKey() {
        properties.setKeyPrefix("/media\\images");
        properties.setPublicBaseUrl("https://cdn.example.com/");
        MockMultipartFile file = new MockMultipartFile("file", "bad name!.png", "image/png", "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        PutObjectRequest request = capturedPutObjectRequest();
        assertThat(request.bucket()).isEqualTo("bucket");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.contentLength()).isEqualTo(4L);
        assertThat(request.key()).startsWith("media/images/");
        assertThat(request.key()).contains("bad_name_.png");
        assertThat(storedMedia.key()).isEqualTo(request.key());
        assertThat(storedMedia.url()).isEqualTo("https://cdn.example.com/" + request.key());
    }

    @Test
    void uploadUsesS3UrlDefaultContentTypeAndDefaultFilename() {
        properties.setKeyPrefix(null);
        properties.setPublicBaseUrl(null);
        MockMultipartFile file = new MockMultipartFile("file", null, null, "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        PutObjectRequest request = capturedPutObjectRequest();
        assertThat(request.contentType()).isEqualTo("application/octet-stream");
        assertThat(request.key()).contains("-upload");
        assertThat(request.key()).doesNotStartWith("/");
        assertThat(storedMedia.url()).isEqualTo("https://bucket.s3.us-east-1.amazonaws.com/" + request.key());
    }

    @Test
    void uploadUsesDefaultFilenameForBlankOriginalFilenameAndBlankBaseUrl() {
        properties.setKeyPrefix(" ");
        properties.setPublicBaseUrl(" ");
        MockMultipartFile file = new MockMultipartFile("file", " ", "image/png", "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        PutObjectRequest request = capturedPutObjectRequest();
        assertThat(request.key()).contains("-upload");
        assertThat(storedMedia.url()).isEqualTo("https://bucket.s3.us-east-1.amazonaws.com/" + request.key());
    }

    @Test
    void uploadKeepsPrefixWithTrailingSlashAndBaseUrlWithoutTrailingSlash() {
        properties.setKeyPrefix("uploads/");
        properties.setPublicBaseUrl("https://cdn.example.com");
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        PutObjectRequest request = capturedPutObjectRequest();
        assertThat(request.key()).startsWith("uploads/");
        assertThat(storedMedia.url()).isEqualTo("https://cdn.example.com/" + request.key());
    }

    @Test
    void uploadStoresFileLocallyWhenBucketIsMissing() throws Exception {
        properties.setBucket(null);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        assertThat(storedMedia.key()).contains("image.png");
        assertThat(storedMedia.url()).isEqualTo("http://localhost:8080/uploads/" + storedMedia.key());
        assertThat(Files.readString(Path.of("uploads").resolve(storedMedia.key()))).isEqualTo("data");
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        Files.deleteIfExists(Path.of("uploads").resolve(storedMedia.key()));
    }

    @Test
    void uploadStoresFileLocallyWhenBucketIsBlank() throws Exception {
        properties.setBucket(" ");
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());

        S3MediaStorageService.StoredMedia storedMedia = storageService.upload(file);

        assertThat(storedMedia.url()).startsWith("http://localhost:8080/uploads/");
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        Files.deleteIfExists(Path.of("uploads").resolve(storedMedia.key()));
    }

    @Test
    void uploadRejectsMissingRegion() {
        properties.setRegion(null);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());

        assertThatThrownBy(() -> storageService.upload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("AWS S3 region is not configured.");
    }

    @Test
    void uploadRejectsBlankRegion() {
        properties.setRegion(" ");
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());

        assertThatThrownBy(() -> storageService.upload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("AWS S3 region is not configured.");
    }

    @Test
    void uploadWrapsFileReadFailure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getSize()).thenReturn(4L);
        when(file.getInputStream()).thenThrow(new IOException("read failed"));

        assertThatThrownBy(() -> storageService.upload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Unable to read uploaded media file.");
    }

    @Test
    void uploadWrapsS3Failure() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getSize()).thenReturn(4L);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(S3Exception.builder().message("s3 failed").build());

        assertThatThrownBy(() -> storageService.upload(file))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Unable to upload media file to S3.");
    }

    private PutObjectRequest capturedPutObjectRequest() {
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(software.amazon.awssdk.core.sync.RequestBody.class));
        return captor.getValue();
    }

    private S3StorageProperties configuredProperties() {
        S3StorageProperties s3StorageProperties = new S3StorageProperties();
        s3StorageProperties.setBucket("bucket");
        s3StorageProperties.setRegion("us-east-1");
        s3StorageProperties.setKeyPrefix("uploads");
        s3StorageProperties.setPublicBaseUrl("https://cdn.example.com");
        return s3StorageProperties;
    }
}
