package com.tinsae.socialmediaplatform.common.mapper;

import com.tinsae.socialmediaplatform.report.dto.ReportResponse;
import com.tinsae.socialmediaplatform.report.entity.Report;

public final class ReportMapper {

    private ReportMapper() {
    }

    public static ReportResponse toResponse(Report report) {
        return new ReportResponse(
                report.getId(),
                UserMapper.toSummary(report.getReporter()),
                report.getResolver() != null ? UserMapper.toSummary(report.getResolver()) : null,
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDetails(),
                report.getResolutionNote(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getResolvedAt()
        );
    }
}
