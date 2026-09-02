package com.tinsae.socialmediaplatform.report.dto;

import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateReportRequest(
        @NotNull(message = "Report target type is required.")
        ReportTargetType targetType,

        @NotNull(message = "Report target ID is required.")
        UUID targetId,

        @NotBlank(message = "Report reason is required.")
        @Size(max = 255, message = "Report reason must not exceed 255 characters.")
        String reason,

        @Size(max = 5000, message = "Report details must not exceed 5000 characters.")
        String details
) {
}
