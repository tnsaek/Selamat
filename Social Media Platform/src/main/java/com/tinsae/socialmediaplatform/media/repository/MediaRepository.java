package com.tinsae.socialmediaplatform.media.repository;

import com.tinsae.socialmediaplatform.common.enums.MediaStatus;
import com.tinsae.socialmediaplatform.media.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaRepository extends JpaRepository<Media, UUID> {

    List<Media> findByPostId(UUID postId);

    List<Media> findByPostIdAndStatus(UUID postId, MediaStatus status);

    List<Media> findByUploaderId(UUID uploaderId);
}
