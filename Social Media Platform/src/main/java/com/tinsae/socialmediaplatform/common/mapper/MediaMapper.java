package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.media.dto.MediaResponse;
import com.tinsae.socialmediaplatform.media.entity.Media;

public final class MediaMapper {

    private MediaMapper() {
    }

    public static MediaResponse toResponse(Media media) {
        return new MediaResponse(
                media.getId(),
                media.getUrl(),
                media.getMediaType(),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getAltText(),
                media.getCreatedAt()
        );
    }
}
