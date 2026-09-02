package com.tinsae.socialmediaplatform.report.dto;

import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID id,
        UserSummaryResponse reporter,
        UserSummaryResponse resolver,
        ReportTargetType targetType,
        UUID targetId,
        String reason,
        String details,
        String resolutionNote,
        ReportStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}
