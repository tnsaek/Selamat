package com.tinsae.socialmediaplatform.report.dto;

import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveReportRequest(
        @NotNull(message = "Report status is required.")
        ReportStatus status,

        @Size(max = 1000, message = "Resolution note must not exceed 1000 characters.")
        String resolutionNote
) {
}
