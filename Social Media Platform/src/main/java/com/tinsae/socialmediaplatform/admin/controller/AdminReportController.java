package com.tinsae.socialmediaplatform.admin.controller;

import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.report.dto.AdminReportSummaryResponse;
import com.tinsae.socialmediaplatform.report.dto.ReportResponse;
import com.tinsae.socialmediaplatform.report.dto.ReportTargetResponse;
import com.tinsae.socialmediaplatform.report.dto.ResolveReportRequest;
import com.tinsae.socialmediaplatform.report.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public PageResponse<ReportResponse> listReports(
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return reportService.listReports(status, cursor, limit);
    }

    @GetMapping("/summary")
    public AdminReportSummaryResponse getAdminSummary() {
        return reportService.getAdminSummary();
    }

    @GetMapping("/{reportId}")
    public ReportResponse getReportById(@PathVariable UUID reportId) {
        return reportService.getReportById(reportId);
    }

    @GetMapping("/{reportId}/target")
    public ReportTargetResponse getReportTarget(@PathVariable UUID reportId) {
        return reportService.getReportTarget(reportId);
    }

    @PatchMapping("/{reportId}/resolve")
    public ReportResponse resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ResolveReportRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return reportService.resolveReport(reportId, request, jwt);
    }
}
