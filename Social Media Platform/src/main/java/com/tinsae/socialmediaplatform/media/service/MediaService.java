package com.tinsae.socialmediaplatform.media.service;

import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.common.enums.MediaType;
import com.tinsae.socialmediaplatform.common.exception.BusinessRuleException;
import com.tinsae.socialmediaplatform.common.exception.ResourceNotFoundException;
import com.tinsae.socialmediaplatform.common.exception.UnauthorizedActionException;
import com.tinsae.socialmediaplatform.common.mapper.MediaMapper;
import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.media.entity.Media;
import com.tinsae.socialmediaplatform.media.repository.MediaRepository;
import com.tinsae.socialmediaplatform.post.entity.Post;
import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final S3MediaStorageService s3MediaStorageService;
    private final UserService userService;

    public MediaService(
            MediaRepository mediaRepository,
            S3MediaStorageService s3MediaStorageService,
            UserService userService
    ) {
        this.mediaRepository = mediaRepository;
        this.s3MediaStorageService = s3MediaStorageService;
        this.userService = userService;
    }

    @Transactional
    public MediaResponse uploadMedia(MultipartFile file, String altText, Jwt jwt) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Media file is required.");
        }

        User uploader = userService.getAuthenticatedUser(jwt);
        String contentType = file.getContentType();
        MediaType mediaType = mediaType(contentType);
        S3MediaStorageService.StoredMedia storedMedia = s3MediaStorageService.upload(file);

        Media media = new Media();
        media.setUploader(uploader);
        media.setUrl(storedMedia.url());
        media.setMediaType(mediaType);
        media.setMimeType(contentType != null ? contentType : "application/octet-stream");
        media.setSizeBytes(file.getSize());
        media.setAltText(altText);

        return MediaMapper.toResponse(mediaRepository.save(media));
    }

    @Transactional(readOnly = true)
    public List<Media> findMediaForPost(UUID postId) {
        return mediaRepository.findByPostIdAndStatus(postId, MediaStatus.VISIBLE);
    }

    @Transactional
    public List<Media> attachMediaToPost(List<UUID> mediaIds, Post post, User owner) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> uniqueMediaIds = new HashSet<>(mediaIds);
        if (uniqueMediaIds.size() != mediaIds.size()) {
            throw new BusinessRuleException("Media IDs must not contain duplicates.");
        }

        List<Media> media = mediaRepository.findAllById(mediaIds);
        if (media.size() != mediaIds.size()) {
            throw new ResourceNotFoundException("One or more media items were not found.");
        }

        for (Media item : media) {
            if (!item.getUploader().getId().equals(owner.getId())) {
                throw new UnauthorizedActionException("You can only attach media that you uploaded.");
            }

            if (item.getPost() != null && !item.getPost().getId().equals(post.getId())) {
                throw new BusinessRuleException("One or more media items are already attached to another post.");
            }

            if (item.getStatus() != MediaStatus.VISIBLE) {
                throw new BusinessRuleException("One or more media items are unavailable.");
            }

            item.setPost(post);
        }

        return mediaRepository.saveAll(media);
    }

    private MediaType mediaType(String contentType) {
        if (contentType == null) {
            return MediaType.DOCUMENT;
        }

        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        return MediaType.DOCUMENT;
    }

}
