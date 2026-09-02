package com.tinsae.socialmediaplatform.common.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        String nextCursor
) {
}
