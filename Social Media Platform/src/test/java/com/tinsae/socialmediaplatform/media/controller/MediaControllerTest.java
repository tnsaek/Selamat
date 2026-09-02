package com.tinsae.socialmediaplatform.media.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.media.service.MediaService;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    @Test
    void uploadMediaReturnsCreatedResponse() {
        User user = TestDataFactory.user("uploader");
        var jwt = TestDataFactory.jwt(user);
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "data".getBytes());
        MediaResponse media = mediaResponse(MediaType.IMAGE);
        when(mediaService.uploadMedia(file, "alt", jwt)).thenReturn(media);

        var response = mediaController.uploadMedia(file, "alt", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(media);
        verify(mediaService).uploadMedia(file, "alt", jwt);
    }

    private MediaResponse mediaResponse(MediaType mediaType) {
        return new MediaResponse(
                UUID.randomUUID(),
                "/uploads/image.png",
                mediaType,
                "image/png",
                4L,
                "alt",
                Instant.now()
        );
    }
}
