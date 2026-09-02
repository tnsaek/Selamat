package com.tinsae.socialmediaplatform.media.service;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private S3MediaStorageService s3MediaStorageService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MediaService mediaService;

    @Test
    void uploadMediaRejectsNullFile() {
        User user = TestDataFactory.user("uploader");
        var jwt = TestDataFactory.jwt(user);

        assertThatThrownBy(() -> mediaService.uploadMedia(null, null, jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Media file is required.");
    }

    @Test
    void uploadMediaRejectsEmptyFile() {
        User user = TestDataFactory.user("uploader");
        var jwt = TestDataFactory.jwt(user);
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> mediaService.uploadMedia(file, null, jwt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Media file is required.");
    }

    @Test
    void uploadMediaStoresImageMediaWithCleanFilename() {
        User uploader = TestDataFactory.user("uploader");
        var jwt = TestDataFactory.jwt(uploader);
        MockMultipartFile file = new MockMultipartFile("file", "bad name!.png", "image/png", "data".getBytes());
        when(userService.getAuthenticatedUser(jwt)).thenReturn(uploader);
        when(s3MediaStorageService.upload(file))
                .thenReturn(new S3MediaStorageService.StoredMedia("uploads/image.png", "https://cdn.example.com/uploads/image.png"));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> savedMedia(invocation.getArgument(0)));

        var response = mediaService.uploadMedia(file, "alt", jwt);

        assertThat(response.mediaType()).isEqualTo(MediaType.IMAGE);
        assertThat(response.mimeType()).isEqualTo("image/png");
        assertThat(response.sizeBytes()).isEqualTo(4L);
        assertThat(response.altText()).isEqualTo("alt");
        assertThat(response.url()).isEqualTo("https://cdn.example.com/uploads/image.png");
    }

    @Test
    void uploadMediaClassifiesVideoAudioDocumentAndNullContentType() {
        assertUploadedType("video/mp4", "video.mp4", MediaType.VIDEO, "video/mp4");
        assertUploadedType("audio/mpeg", "audio.mp3", MediaType.AUDIO, "audio/mpeg");
        assertUploadedType("application/pdf", "doc.pdf", MediaType.DOCUMENT, "application/pdf");
        assertUploadedType(null, "   ", MediaType.DOCUMENT, "application/octet-stream");
        assertUploadedType(null, null, MediaType.DOCUMENT, "application/octet-stream");
    }

    @Test
    void findMediaForPostDelegatesToRepository() {
        UUID postId = UUID.randomUUID();
        List<Media> media = List.of(media(TestDataFactory.user("uploader"), null));
        when(mediaRepository.findByPostIdAndStatus(postId, MediaStatus.VISIBLE)).thenReturn(media);

        var response = mediaService.findMediaForPost(postId);

        assertThat(response).isSameAs(media);
        verify(mediaRepository).findByPostIdAndStatus(postId, MediaStatus.VISIBLE);
    }

    @Test
    void attachMediaToPostReturnsEmptyListForNullOrEmptyIds() {
        Post post = TestDataFactory.post(TestDataFactory.user("owner"));
        User owner = post.getAuthor();

        assertThat(mediaService.attachMediaToPost(null, post, owner)).isEmpty();
        assertThat(mediaService.attachMediaToPost(List.of(), post, owner)).isEmpty();
    }

    @Test
    void attachMediaToPostRejectsMissingMedia() {
        Post post = TestDataFactory.post(TestDataFactory.user("owner"));
        User owner = post.getAuthor();
        List<UUID> mediaIds = List.of(UUID.randomUUID());
        when(mediaRepository.findAllById(mediaIds)).thenReturn(List.of());

        assertThatThrownBy(() -> mediaService.attachMediaToPost(mediaIds, post, owner))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("One or more media items were not found.");
    }

    @Test
    void attachMediaToPostRejectsDuplicateMediaIds() {
        Post post = TestDataFactory.post(TestDataFactory.user("owner"));
        User owner = post.getAuthor();
        UUID mediaId = UUID.randomUUID();
        List<UUID> mediaIds = List.of(mediaId, mediaId);

        assertThatThrownBy(() -> mediaService.attachMediaToPost(mediaIds, post, owner))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Media IDs must not contain duplicates.");
    }

    @Test
    void attachMediaToPostRejectsMediaFromAnotherUploader() {
        User owner = TestDataFactory.user("owner");
        User other = TestDataFactory.user("other");
        Post post = TestDataFactory.post(owner);
        Media media = media(other, null);
        List<UUID> mediaIds = List.of(media.getId());
        when(mediaRepository.findAllById(mediaIds)).thenReturn(List.of(media));

        assertThatThrownBy(() -> mediaService.attachMediaToPost(mediaIds, post, owner))
                .isInstanceOf(UnauthorizedActionException.class)
                .hasMessage("You can only attach media that you uploaded.");
    }

    @Test
    void attachMediaToPostRejectsMediaAttachedToAnotherPost() {
        User owner = TestDataFactory.user("owner");
        Post post = TestDataFactory.post(owner);
        Post otherPost = TestDataFactory.post(owner);
        Media media = media(owner, otherPost);
        List<UUID> mediaIds = List.of(media.getId());
        when(mediaRepository.findAllById(mediaIds)).thenReturn(List.of(media));

        assertThatThrownBy(() -> mediaService.attachMediaToPost(mediaIds, post, owner))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("One or more media items are already attached to another post.");
    }

    @Test
    void attachMediaToPostRejectsUnavailableMedia() {
        User owner = TestDataFactory.user("owner");
        Post post = TestDataFactory.post(owner);
        Media media = media(owner, null);
        media.setStatus(MediaStatus.HIDDEN);
        List<UUID> mediaIds = List.of(media.getId());
        when(mediaRepository.findAllById(mediaIds)).thenReturn(List.of(media));

        assertThatThrownBy(() -> mediaService.attachMediaToPost(mediaIds, post, owner))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("One or more media items are unavailable.");
    }

    @Test
    void attachMediaToPostSavesUnattachedAndAlreadyAttachedToSamePostMedia() {
        User owner = TestDataFactory.user("owner");
        Post post = TestDataFactory.post(owner);
        Media unattached = media(owner, null);
        Media alreadyAttached = media(owner, post);
        List<UUID> mediaIds = List.of(unattached.getId(), alreadyAttached.getId());
        List<Media> media = List.of(unattached, alreadyAttached);
        when(mediaRepository.findAllById(mediaIds)).thenReturn(media);
        when(mediaRepository.saveAll(media)).thenReturn(media);

        var response = mediaService.attachMediaToPost(mediaIds, post, owner);

        assertThat(response).containsExactlyElementsOf(media);
        assertThat(unattached.getPost()).isSameAs(post);
        assertThat(alreadyAttached.getPost()).isSameAs(post);
        verify(mediaRepository).saveAll(media);
    }

    private void assertUploadedType(String contentType, String filename, MediaType expectedType, String expectedMimeType) {
        User uploader = TestDataFactory.user("uploader-" + expectedType.name().toLowerCase());
        var jwt = TestDataFactory.jwt(uploader);
        MockMultipartFile file = new MockMultipartFile("file", filename, contentType, "data".getBytes());
        when(userService.getAuthenticatedUser(jwt)).thenReturn(uploader);
        when(s3MediaStorageService.upload(file))
                .thenReturn(new S3MediaStorageService.StoredMedia("uploads/file", "https://cdn.example.com/uploads/file"));
        when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> savedMedia(invocation.getArgument(0)));

        var response = mediaService.uploadMedia(file, null, jwt);

        assertThat(response.mediaType()).isEqualTo(expectedType);
        assertThat(response.mimeType()).isEqualTo(expectedMimeType);
    }

    private Media savedMedia(Media media) {
        media.setId(UUID.randomUUID());
        media.setCreatedAt(Instant.now());
        return media;
    }

    private Media media(User uploader, Post post) {
        Media media = new Media();
        media.setId(UUID.randomUUID());
        media.setUploader(uploader);
        media.setPost(post);
        media.setUrl("/uploads/file.png");
        media.setMediaType(MediaType.IMAGE);
        media.setMimeType("image/png");
        media.setSizeBytes(4L);
        media.setAltText("alt");
        media.setStatus(MediaStatus.VISIBLE);
        media.setCreatedAt(Instant.now());
        return media;
    }
}
