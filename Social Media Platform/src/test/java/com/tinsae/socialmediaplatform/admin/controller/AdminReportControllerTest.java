package com.tinsae.socialmediaplatform.admin.controller;

import com.tinsae.socialmediaplatform.TestDataFactory;
import com.tinsae.socialmediaplatform.common.dto.PageResponse;
import com.tinsae.socialmediaplatform.common.enums.ReportStatus;
import com.tinsae.socialmediaplatform.common.enums.ReportTargetType;
import com.tinsae.socialmediaplatform.report.dto.AdminReportSummaryResponse;
import com.tinsae.socialmediaplatform.report.dto.ReportResponse;
import com.tinsae.socialmediaplatform.report.dto.ReportTargetResponse;
import com.tinsae.socialmediaplatform.report.dto.ResolveReportRequest;
import com.tinsae.socialmediaplatform.report.service.ReportService;
import com.tinsae.socialmediaplatform.user.dto.UserSummaryResponse;
import com.tinsae.socialmediaplatform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private AdminReportController adminReportController;

    @Test
    void listReportsDelegatesToReportService() {
        ReportResponse report = reportResponse(ReportStatus.OPEN);
        PageResponse<ReportResponse> page = new PageResponse<>(List.of(report), null);
        when(reportService.listReports(ReportStatus.OPEN, "cursor", 25)).thenReturn(page);

        PageResponse<ReportResponse> response = adminReportController.listReports(ReportStatus.OPEN, "cursor", 25);

        assertThat(response).isSameAs(page);
        assertThat(response.items()).containsExactly(report);
        verify(reportService).listReports(ReportStatus.OPEN, "cursor", 25);
    }

    @Test
    void getAdminSummaryDelegatesToReportService() {
        AdminReportSummaryResponse summary = new AdminReportSummaryResponse(2, 1, 4, 3, 5, 6, 7);
        when(reportService.getAdminSummary()).thenReturn(summary);

        AdminReportSummaryResponse response = adminReportController.getAdminSummary();

        assertThat(response).isSameAs(summary);
        verify(reportService).getAdminSummary();
    }

    @Test
    void resolveReportDelegatesToReportService() {
        User moderator = TestDataFactory.user("moderator");
        var jwt = TestDataFactory.jwt(moderator);
        UUID reportId = UUID.randomUUID();
        ResolveReportRequest request = new ResolveReportRequest(ReportStatus.RESOLVED, "Handled");
        ReportResponse resolvedReport = reportResponse(ReportStatus.RESOLVED);
        when(reportService.resolveReport(reportId, request, jwt)).thenReturn(resolvedReport);

        ReportResponse response = adminReportController.resolveReport(reportId, request, jwt);

        assertThat(response).isSameAs(resolvedReport);
        assertThat(response.status()).isEqualTo(ReportStatus.RESOLVED);
        verify(reportService).resolveReport(reportId, request, jwt);
    }

    @Test
    void getReportByIdDelegatesToReportService() {
        UUID reportId = UUID.randomUUID();
        ReportResponse report = reportResponse(ReportStatus.OPEN);
        when(reportService.getReportById(reportId)).thenReturn(report);

        ReportResponse response = adminReportController.getReportById(reportId);

        assertThat(response).isSameAs(report);
        verify(reportService).getReportById(reportId);
    }

    @Test
    void getReportTargetDelegatesToReportService() {
        UUID reportId = UUID.randomUUID();
        ReportTargetResponse target = new ReportTargetResponse(
                reportId,
                ReportTargetType.POST,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Reported content",
                "PUBLISHED",
                "PUBLIC",
                0L,
                0L,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                List.of()
        );
        when(reportService.getReportTarget(reportId)).thenReturn(target);

        ReportTargetResponse response = adminReportController.getReportTarget(reportId);

        assertThat(response).isSameAs(target);
        verify(reportService).getReportTarget(reportId);
    }

    private ReportResponse reportResponse(ReportStatus status) {
        return new ReportResponse(
                UUID.randomUUID(),
                new UserSummaryResponse(UUID.randomUUID(), "reporter", "Reporter", null),
                status == ReportStatus.OPEN
                        ? null
                        : new UserSummaryResponse(UUID.randomUUID(), "resolver", "Resolver", null),
                ReportTargetType.POST,
                UUID.randomUUID(),
                "Spam",
                "Details",
                null,
                status,
                Instant.now(),
                status == ReportStatus.OPEN ? null : Instant.now()
        );
    }
}
