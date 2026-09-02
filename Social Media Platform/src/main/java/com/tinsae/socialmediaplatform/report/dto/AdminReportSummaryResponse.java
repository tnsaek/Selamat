package com.tinsae.socialmediaplatform.report.dto;

public record AdminReportSummaryResponse(
        long openReports,
        long underReviewReports,
        long resolvedReports,
        long rejectedReports,
        long userReports,
        long contentReports,
        long mediaReports
) {
}
